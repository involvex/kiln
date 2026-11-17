package dev.involvex.kiln.feature.widgets.glance

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import dagger.hilt.android.AndroidEntryPoint
import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.core.model.ActivityLogEvents
import dev.involvex.kiln.core.model.ActivityLogRecorder
import dev.involvex.kiln.core.model.ActivityLogType
import dev.involvex.kiln.core.model.BackgroundUpdateScheduler
import dev.involvex.kiln.core.model.CacheClearConfirmGate
import dev.involvex.kiln.core.model.ForceStopConfirmGate
import dev.involvex.kiln.core.model.PrivilegeCapability
import dev.involvex.kiln.core.model.PrivilegedExecutor
import dev.involvex.kiln.core.model.ProcessStatsGate
import dev.involvex.kiln.core.model.WidgetIntents
import dev.involvex.kiln.core.model.WidgetPreferences
import dev.involvex.kiln.feature.widgets.ManageDeepLinkStore
import dev.involvex.kiln.feature.widgets.PrivilegeSetupDeepLinkStore
import dev.involvex.kiln.feature.widgets.ProcessWidgetRefresher
import dev.involvex.kiln.feature.widgets.R
import dev.involvex.kiln.feature.widgets.WidgetSurfaceUpdater
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

@AndroidEntryPoint
class WidgetActionReceiver : BroadcastReceiver() {
    @Inject lateinit var scheduler: BackgroundUpdateScheduler

    @Inject lateinit var privilegedExecutor: PrivilegedExecutor

    @Inject lateinit var widgetPreferences: WidgetPreferences

    @Inject lateinit var surfaceUpdater: WidgetSurfaceUpdater

    @Inject lateinit var manageDeepLinkStore: ManageDeepLinkStore

    @Inject lateinit var privilegeSetupDeepLinkStore: PrivilegeSetupDeepLinkStore

    @Inject lateinit var processWidgetRefresher: ProcessWidgetRefresher

    @Inject lateinit var activityLog: ActivityLogRecorder

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (action) {
                    WidgetIntents.ACTION_CHECK_NOW -> {
                        scheduler.enqueueImmediate()
                        widgetPreferences.setCheckQueued(true)
                        surfaceUpdater.refreshUpdates()
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context.applicationContext,
                                context.getString(R.string.widget_updates_queued),
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }

                    WidgetIntents.ACTION_CACHE_REQUEST -> {
                        widgetPreferences.setCacheConfirmPending(true)
                        surfaceUpdater.refreshCache()
                    }

                    WidgetIntents.ACTION_CACHE_CANCEL -> {
                        widgetPreferences.setCacheConfirmPending(false)
                        surfaceUpdater.refreshCache()
                    }

                    WidgetIntents.ACTION_CACHE_CONFIRM -> {
                        val snap = widgetPreferences.current()
                        if (!CacheClearConfirmGate.mayExecute(snap.cacheConfirmPending)) {
                            Timber.w("Cache confirm ignored: not pending")
                        } else {
                            widgetPreferences.setCacheConfirmPending(false)
                            clearCaches(context)
                        }
                        surfaceUpdater.refreshCache()
                    }

                    WidgetIntents.ACTION_PROCESS_REFRESH -> {
                        processWidgetRefresher.refreshIfStale(force = true)
                        surfaceUpdater.refreshProcesses()
                    }

                    WidgetIntents.ACTION_KILL_REQUEST -> {
                        val pkg = intent.getStringExtra(WidgetIntents.EXTRA_PACKAGE_NAME)
                        if (!pkg.isNullOrBlank()) {
                            widgetPreferences.setKillConfirmPackage(pkg)
                            surfaceUpdater.refreshProcesses()
                        }
                    }

                    WidgetIntents.ACTION_KILL_CANCEL -> {
                        widgetPreferences.setKillConfirmPackage(null)
                        surfaceUpdater.refreshProcesses()
                    }

                    WidgetIntents.ACTION_KILL_CONFIRM -> {
                        val pkg = intent.getStringExtra(WidgetIntents.EXTRA_PACKAGE_NAME).orEmpty()
                        val snap = widgetPreferences.current()
                        if (!ForceStopConfirmGate.mayExecute(
                                pendingPackage = snap.killConfirmPackage,
                                targetPackage = pkg,
                                kilnPackageName = context.packageName,
                            )
                        ) {
                            Timber.w("Kill confirm ignored for %s", pkg)
                            widgetPreferences.setKillConfirmPackage(null)
                        } else {
                            widgetPreferences.setKillConfirmPackage(null)
                            forceStop(context, pkg)
                        }
                        processWidgetRefresher.refreshIfStale(force = true)
                        surfaceUpdater.refreshProcesses()
                    }

                    // Legacy Updates-widget trim actions: ignore (trim lives on Cache widget).
                    WidgetIntents.ACTION_TRIM_REQUEST,
                    WidgetIntents.ACTION_TRIM_CONFIRM,
                    WidgetIntents.ACTION_TRIM_CANCEL,
                    -> Unit
                }
            } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
                Timber.w(error, "Widget action failed: %s", action)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun clearCaches(context: Context) {
        val canClear = PrivilegeCapability.CLEAR_CACHE in privilegedExecutor.currentStatus().capabilities
        if (!canClear) {
            openManageAndPrivilegeSetup(context)
            return
        }
        when (val result = privilegedExecutor.clearCache(null)) {
            is Result.Failure -> {
                activityLog.record(
                    ActivityLogEvents.create(
                        type = ActivityLogType.CACHE,
                        action = "clear_cache",
                        success = false,
                        detailKey = result.message,
                    ),
                )
                Timber.w("Widget cache clear failed: %s", result.message)
            }

            is Result.Success -> {
                activityLog.record(
                    ActivityLogEvents.create(
                        type = ActivityLogType.CACHE,
                        action = "clear_cache",
                        success = true,
                        detailKey = "bytes:${result.value}",
                    ),
                )
            }
        }
    }

    private suspend fun forceStop(context: Context, packageName: String) {
        val caps = privilegedExecutor.currentStatus().capabilities
        if (!ProcessStatsGate.canForceStop(caps)) {
            privilegeSetupDeepLinkStore.offer()
            openPrivilegeSetup(context)
            return
        }
        when (val result = privilegedExecutor.forceStopPackage(packageName)) {
            is Result.Failure -> {
                activityLog.record(
                    ActivityLogEvents.create(
                        type = ActivityLogType.SHELL,
                        action = "force_stop",
                        success = false,
                        packageName = packageName,
                        detailKey = result.message,
                    ),
                )
                Timber.w("Widget force-stop failed: %s", result.message)
            }

            is Result.Success -> {
                activityLog.record(
                    ActivityLogEvents.create(
                        type = ActivityLogType.SHELL,
                        action = "force_stop",
                        success = true,
                        packageName = packageName,
                    ),
                )
            }
        }
    }

    private fun openManageAndPrivilegeSetup(context: Context) {
        manageDeepLinkStore.offer()
        privilegeSetupDeepLinkStore.offer()
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            ?.putExtra(WidgetIntents.EXTRA_OPEN_MANAGE, true)
            ?.putExtra(WidgetIntents.EXTRA_OPEN_PRIVILEGE_SETUP, true)
        if (launch != null) {
            context.startActivity(launch)
        }
    }

    private fun openPrivilegeSetup(context: Context) {
        privilegeSetupDeepLinkStore.offer()
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            ?.putExtra(WidgetIntents.EXTRA_OPEN_PRIVILEGE_SETUP, true)
        if (launch != null) {
            context.startActivity(launch)
        }
    }
}

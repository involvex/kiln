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
import dev.involvex.kiln.core.model.PrivilegeCapability
import dev.involvex.kiln.core.model.PrivilegedExecutor
import dev.involvex.kiln.core.model.WidgetIntents
import dev.involvex.kiln.core.model.WidgetPreferences
import dev.involvex.kiln.feature.widgets.ManageDeepLinkStore
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
                        surfaceUpdater.refreshAll()
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context.applicationContext,
                                context.getString(R.string.widget_updates_queued),
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }

                    WidgetIntents.ACTION_TRIM_REQUEST -> {
                        widgetPreferences.setTrimConfirmPending(true)
                        surfaceUpdater.refreshAll()
                    }

                    WidgetIntents.ACTION_TRIM_CANCEL -> {
                        widgetPreferences.setTrimConfirmPending(false)
                        surfaceUpdater.refreshAll()
                    }

                    WidgetIntents.ACTION_TRIM_CONFIRM -> {
                        widgetPreferences.setTrimConfirmPending(false)
                        trimCaches(context)
                        surfaceUpdater.refreshAll()
                    }
                }
            } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
                Timber.w(error, "Widget action failed: %s", action)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun trimCaches(context: Context) {
        val canClear = PrivilegeCapability.CLEAR_CACHE in privilegedExecutor.currentStatus().capabilities
        if (!canClear) {
            manageDeepLinkStore.offer()
            val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
                ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                ?.putExtra(WidgetIntents.EXTRA_OPEN_MANAGE, true)
            if (launch != null) {
                context.startActivity(launch)
            }
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
                Timber.w("Widget trim failed: %s", result.message)
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
}

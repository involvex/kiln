package dev.involvex.kiln.feature.installer.session

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import dagger.hilt.android.AndroidEntryPoint
import dev.involvex.kiln.core.database.InstallJobDao
import dev.involvex.kiln.core.model.ActivityLogEvents
import dev.involvex.kiln.core.model.ActivityLogRecorder
import dev.involvex.kiln.core.model.ActivityLogType
import dev.involvex.kiln.core.model.ApkRollbackCache
import dev.involvex.kiln.core.model.InstallJobStatus
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class InstallStatusReceiver : BroadcastReceiver() {
    @Inject lateinit var installJobDao: InstallJobDao

    @Inject lateinit var rollbackCache: ApkRollbackCache

    @Inject lateinit var activityLog: ActivityLogRecorder

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_INSTALL_STATUS) return
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        val jobId = intent.getStringExtra(EXTRA_JOB_ID) ?: return
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (status) {
                    PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                        val confirm = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(Intent.EXTRA_INTENT)
                        }
                        if (confirm != null) {
                            confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(confirm)
                        }
                        installJobDao.updateProgress(
                            jobId = jobId,
                            status = InstallJobStatus.INSTALLING.name,
                            progressPercent = 95,
                            errorMessage = null,
                            localApkPath = installJobDao.get(jobId)?.localApkPath,
                            updatedAtEpochMs = System.currentTimeMillis(),
                        )
                    }

                    PackageInstaller.STATUS_SUCCESS -> {
                        val job = installJobDao.get(jobId)
                        installJobDao.updateProgress(
                            jobId = jobId,
                            status = InstallJobStatus.SUCCEEDED.name,
                            progressPercent = 100,
                            errorMessage = null,
                            localApkPath = job?.localApkPath,
                            updatedAtEpochMs = System.currentTimeMillis(),
                        )
                        val path = job?.localApkPath
                        if (job != null && !path.isNullOrBlank() && File(path).isFile) {
                            rollbackCache.rememberSuccessfulInstall(
                                packageName = job.packageName,
                                versionName = job.versionName,
                                versionCode = job.versionCode,
                                apkAbsolutePath = path,
                            )
                        }
                        activityLog.record(
                            ActivityLogEvents.create(
                                type = ActivityLogType.INSTALL,
                                action = "install_succeeded",
                                success = true,
                                packageName = job?.packageName,
                                versionName = job?.versionName,
                                versionCode = job?.versionCode,
                                refId = jobId,
                            ),
                        )
                    }

                    else -> {
                        val key = InstallStatusMapper.messageKey(status)
                        Timber.w("Install failed job=%s status=%s msg=%s", jobId, key, message)
                        val job = installJobDao.get(jobId)
                        installJobDao.updateProgress(
                            jobId = jobId,
                            status = InstallJobStatus.FAILED.name,
                            progressPercent = 100,
                            errorMessage = key,
                            localApkPath = job?.localApkPath,
                            updatedAtEpochMs = System.currentTimeMillis(),
                        )
                        activityLog.record(
                            ActivityLogEvents.create(
                                type = ActivityLogType.INSTALL,
                                action = "install_failed",
                                success = false,
                                packageName = job?.packageName,
                                versionName = job?.versionName,
                                versionCode = job?.versionCode,
                                detailKey = key,
                                refId = jobId,
                            ),
                        )
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_INSTALL_STATUS = "dev.involvex.kiln.installer.INSTALL_STATUS"
        const val EXTRA_JOB_ID = "job_id"
        const val EXTRA_PACKAGE_NAME = "package_name"
    }
}

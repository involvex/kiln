package dev.involvex.kiln.feature.apps.export

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.involvex.kiln.core.model.ApkExportFormat
import dev.involvex.kiln.feature.apps.R
import dev.involvex.kiln.feature.apps.data.InstalledAppsRepository
import timber.log.Timber

@HiltWorker
class BatchExportWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: InstalledAppsRepository,
    private val apkExporter: ApkExporter,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val input = parseInput() ?: return Result.failure(workDataOf(KEY_ERROR to "invalid_input"))
        ensureChannel()
        setForeground(createForegroundInfo(0, input.packageNames.size, ""))

        val tree = DocumentFile.fromTreeUri(appContext, input.treeUri)
        if (tree == null) {
            return Result.failure(workDataOf(KEY_ERROR to "invalid_tree"))
        }

        val outcome = exportAll(tree, input)
        notifyFinished(
            appContext.getString(R.string.feature_apps_batch_summary, outcome.success, outcome.failed),
        )
        return if (outcome.cancelled) {
            Result.failure(
                workDataOf(
                    KEY_SUCCESS_COUNT to outcome.success,
                    KEY_FAILED_COUNT to outcome.failed,
                    KEY_CANCELLED to true,
                ),
            )
        } else {
            Result.success(
                workDataOf(
                    KEY_SUCCESS_COUNT to outcome.success,
                    KEY_FAILED_COUNT to outcome.failed,
                    KEY_FAILURE_MESSAGES to outcome.failures.toTypedArray(),
                ),
            )
        }
    }

    private data class Input(val treeUri: Uri, val packageNames: List<String>, val format: ApkExportFormat)

    private data class Outcome(val success: Int, val failed: Int, val failures: List<String>, val cancelled: Boolean)

    private fun parseInput(): Input? {
        val tree = inputData.getString(KEY_TREE_URI) ?: return null
        val packages = inputData.getStringArray(KEY_PACKAGE_NAMES)?.toList().orEmpty()
        if (packages.isEmpty()) return null
        val formatName = inputData.getString(KEY_FORMAT) ?: ApkExportFormat.APK.name
        val format = runCatching { ApkExportFormat.valueOf(formatName) }.getOrDefault(ApkExportFormat.APK)
        return Input(Uri.parse(tree), packages, format)
    }

    private suspend fun exportAll(tree: DocumentFile, input: Input): Outcome {
        var success = 0
        var failed = 0
        val failures = mutableListOf<String>()
        input.packageNames.forEachIndexed { index, packageName ->
            if (isStopped) {
                return Outcome(success, failed, failures, cancelled = true)
            }
            setForeground(createForegroundInfo(index, input.packageNames.size, packageName))
            setProgress(
                workDataOf(
                    KEY_PROGRESS_CURRENT to index,
                    KEY_PROGRESS_TOTAL to input.packageNames.size,
                    KEY_PROGRESS_PACKAGE to packageName,
                ),
            )
            when (val single = exportOne(tree, packageName, input.format)) {
                null -> {
                    success += 1
                }

                else -> {
                    failed += 1
                    failures += single
                }
            }
        }
        return Outcome(success, failed, failures, cancelled = false)
    }

    private suspend fun exportOne(tree: DocumentFile, packageName: String, format: ApkExportFormat): String? {
        val detail = repository.detail(packageName).getOrNull()
            ?: return "$packageName: not found"
        val fileName = buildExportFilename(detail.label, detail.versionName, detail.versionCode, format)
        val target = tree.createFile(mimeFor(format), fileName.substringBeforeLast('.'))
            ?: tree.createFile(mimeFor(format), fileName)
            ?: return "$packageName: create_failed"
        runCatching { target.renameTo(fileName) }
        val exportResult = apkExporter.exportToUri(detail, format, target.uri, appContext.contentResolver)
        return if (exportResult.isSuccess) {
            null
        } else {
            runCatching { target.delete() }
            "$packageName: ${exportResult.errorMessageOrNull() ?: "failed"}"
        }
    }

    private fun mimeFor(format: ApkExportFormat): String = when (format) {
        ApkExportFormat.APK -> "application/vnd.android.package-archive"
        ApkExportFormat.APKS, ApkExportFormat.XAPK -> "application/zip"
    }

    private fun createForegroundInfo(current: Int, total: Int, packageName: String): ForegroundInfo {
        val title = appContext.getString(R.string.feature_apps_batch_notification_title)
        val text = appContext.getString(
            R.string.feature_apps_batch_notification_progress,
            current.coerceAtMost(total),
            total,
        )
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$text\n$packageName"))
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(total, current, false)
            .build()

        // API 34+: foreground service type required for long-running workers.
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ForegroundInfo(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = appContext.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            appContext.getString(R.string.feature_apps_batch_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        )
        manager.createNotificationChannel(channel)
    }

    private fun notifyFinished(summary: String) {
        if (!canPostNotifications()) {
            // Denial must not block export; skip the completion notification.
            return
        }
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setContentTitle(appContext.getString(R.string.feature_apps_batch_notification_done))
            .setContentText(summary)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(appContext).notify(NOTIFICATION_DONE_ID, notification)
        } catch (error: SecurityException) {
            Timber.w(error, "Cannot post batch export notification")
        }
    }

    private fun canPostNotifications(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        const val WORK_NAME = "kiln_batch_apk_export"
        const val KEY_TREE_URI = "tree_uri"
        const val KEY_PACKAGE_NAMES = "package_names"
        const val KEY_FORMAT = "format"
        const val KEY_PROGRESS_CURRENT = "progress_current"
        const val KEY_PROGRESS_TOTAL = "progress_total"
        const val KEY_PROGRESS_PACKAGE = "progress_package"
        const val KEY_SUCCESS_COUNT = "success_count"
        const val KEY_FAILED_COUNT = "failed_count"
        const val KEY_FAILURE_MESSAGES = "failure_messages"
        const val KEY_CANCELLED = "cancelled"
        const val KEY_ERROR = "error"

        private const val CHANNEL_ID = "kiln_apk_export"
        private const val NOTIFICATION_ID = 4101
        private const val NOTIFICATION_DONE_ID = 4102
    }
}

package dev.involvex.kiln.feature.maintenance

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.core.model.ApkDownloadJanitor
import dev.involvex.kiln.core.model.ApkRollbackCache
import dev.involvex.kiln.core.model.MaintenanceDefaults
import dev.involvex.kiln.core.model.MaintenancePrefs
import dev.involvex.kiln.core.model.MaintenanceRunRecord
import dev.involvex.kiln.core.model.MaintenanceTaskResult
import dev.involvex.kiln.core.model.PrivilegeCapability
import dev.involvex.kiln.core.model.PrivilegedExecutor
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MaintenanceRunner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val privilegedExecutor: PrivilegedExecutor,
    private val downloadJanitor: ApkDownloadJanitor,
    private val rollbackCache: ApkRollbackCache,
) {
    suspend fun run(prefs: MaintenancePrefs, nowEpochMs: Long = System.currentTimeMillis()): MaintenanceRunRecord {
        val started = nowEpochMs
        val tasks = mutableListOf<MaintenanceTaskResult>()
        if (prefs.trimCaches) {
            tasks += runTrim()
        }
        if (prefs.pruneDownloads) {
            tasks += runPruneDownloads(nowEpochMs)
        }
        if (prefs.pruneRollback) {
            tasks += runPruneRollback(nowEpochMs)
        }
        if (prefs.pruneBackupTemp) {
            tasks += runPruneBackupTemp()
        }
        val finished = System.currentTimeMillis()
        val ok = tasks.none { it.status == MaintenanceDefaults.STATUS_FAILED }
        return MaintenanceRunRecord(
            startedAtEpochMs = started,
            finishedAtEpochMs = finished,
            ok = ok,
            tasks = tasks,
        )
    }

    private suspend fun runTrim(): MaintenanceTaskResult {
        val hasCapability = PrivilegeCapability.CLEAR_CACHE in privilegedExecutor.currentStatus().capabilities
        if (!hasCapability) {
            return MaintenanceTaskResult(
                taskId = MaintenanceDefaults.TASK_TRIM,
                status = MaintenanceDefaults.STATUS_SKIPPED,
                detail = "skipped_no_privilege",
            )
        }
        return when (val result = privilegedExecutor.clearCache(null)) {
            is Result.Failure -> MaintenanceTaskResult(
                taskId = MaintenanceDefaults.TASK_TRIM,
                status = MaintenanceDefaults.STATUS_FAILED,
                detail = result.message,
            )

            is Result.Success -> MaintenanceTaskResult(
                taskId = MaintenanceDefaults.TASK_TRIM,
                status = MaintenanceDefaults.STATUS_OK,
                detail = "trim_caches",
                bytesFreed = result.value,
            )
        }
    }

    private suspend fun runPruneDownloads(nowEpochMs: Long): MaintenanceTaskResult {
        val maxAge = TimeUnit.DAYS.toMillis(MaintenanceDefaults.DOWNLOAD_MAX_AGE_DAYS)
        return try {
            val freed = downloadJanitor.pruneOlderThan(maxAge, nowEpochMs)
            MaintenanceTaskResult(
                taskId = MaintenanceDefaults.TASK_DOWNLOADS,
                status = MaintenanceDefaults.STATUS_OK,
                detail = "apk_downloads",
                bytesFreed = freed,
            )
        } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
            MaintenanceTaskResult(
                taskId = MaintenanceDefaults.TASK_DOWNLOADS,
                status = MaintenanceDefaults.STATUS_FAILED,
                detail = error.message ?: "prune_downloads_failed",
            )
        }
    }

    private suspend fun runPruneRollback(nowEpochMs: Long): MaintenanceTaskResult {
        val maxAge = TimeUnit.DAYS.toMillis(MaintenanceDefaults.ROLLBACK_MAX_AGE_DAYS)
        return try {
            val freed = rollbackCache.pruneByAgeAndSize(
                maxAgeMs = maxAge,
                maxTotalBytes = MaintenanceDefaults.ROLLBACK_MAX_TOTAL_BYTES,
                nowEpochMs = nowEpochMs,
            )
            MaintenanceTaskResult(
                taskId = MaintenanceDefaults.TASK_ROLLBACK,
                status = MaintenanceDefaults.STATUS_OK,
                detail = "apk_rollback",
                bytesFreed = freed,
            )
        } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
            MaintenanceTaskResult(
                taskId = MaintenanceDefaults.TASK_ROLLBACK,
                status = MaintenanceDefaults.STATUS_FAILED,
                detail = error.message ?: "prune_rollback_failed",
            )
        }
    }

    private fun runPruneBackupTemp(): MaintenanceTaskResult = try {
        val dir = File(context.cacheDir, BACKUP_RESTORE_DIR)
        val freed = CacheDirJanitor.deleteContents(dir)
        MaintenanceTaskResult(
            taskId = MaintenanceDefaults.TASK_BACKUP_TEMP,
            status = MaintenanceDefaults.STATUS_OK,
            detail = "backup_restore",
            bytesFreed = freed,
        )
    } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
        MaintenanceTaskResult(
            taskId = MaintenanceDefaults.TASK_BACKUP_TEMP,
            status = MaintenanceDefaults.STATUS_FAILED,
            detail = error.message ?: "prune_backup_temp_failed",
        )
    }

    companion object {
        const val BACKUP_RESTORE_DIR = "backup_restore"
    }
}

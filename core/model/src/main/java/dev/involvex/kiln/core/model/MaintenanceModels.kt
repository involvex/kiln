package dev.involvex.kiln.core.model

import kotlinx.coroutines.flow.Flow

/**
 * User preferences for scheduled maintenance.
 * Defaults: disabled; when enabled, Wi‑Fi + charging and all tasks on.
 */
data class MaintenancePrefs(
    val enabled: Boolean = false,
    val trimCaches: Boolean = true,
    val pruneDownloads: Boolean = true,
    val pruneRollback: Boolean = true,
    val pruneBackupTemp: Boolean = true,
)

data class MaintenanceTaskResult(val taskId: String, val status: String, val detail: String = "", val bytesFreed: Long = 0L)

data class MaintenanceRunRecord(
    val startedAtEpochMs: Long,
    val finishedAtEpochMs: Long,
    val ok: Boolean,
    val tasks: List<MaintenanceTaskResult>,
)

/**
 * Persisted prefs (DataStore). Implemented in `:feature:settings`, bound in `:app`.
 */
interface MaintenancePreferences {
    fun observe(): Flow<MaintenancePrefs>

    suspend fun current(): MaintenancePrefs

    suspend fun setEnabled(enabled: Boolean)

    suspend fun setTrimCaches(enabled: Boolean)

    suspend fun setPruneDownloads(enabled: Boolean)

    suspend fun setPruneRollback(enabled: Boolean)

    suspend fun setPruneBackupTemp(enabled: Boolean)
}

/**
 * Schedules / cancels WorkManager periodic + one-shot maintenance.
 * Implemented in `:feature:maintenance`, bound in `:app`.
 */
interface MaintenanceScheduler {
    fun apply(prefs: MaintenancePrefs)

    fun enqueueImmediate()
}

/**
 * Append-only maintenance protocol. Implemented in `:feature:maintenance`.
 */
interface MaintenanceRunStore {
    fun observe(): Flow<List<MaintenanceRunRecord>>

    suspend fun latest(): MaintenanceRunRecord?

    suspend fun append(record: MaintenanceRunRecord)
}

/** Constants shared by worker, janitors, and UI. */
object MaintenanceDefaults {
    const val INTERVAL_DAYS = 7L
    const val DOWNLOAD_MAX_AGE_DAYS = 3L
    const val ROLLBACK_MAX_AGE_DAYS = 30L
    const val ROLLBACK_MAX_TOTAL_BYTES = 1L * 1024L * 1024L * 1024L
    const val MAX_RUN_RECORDS = 50
    const val TASK_TRIM = "trim_caches"
    const val TASK_DOWNLOADS = "prune_downloads"
    const val TASK_ROLLBACK = "prune_rollback"
    const val TASK_BACKUP_TEMP = "prune_backup_temp"
    const val STATUS_OK = "ok"
    const val STATUS_SKIPPED = "skipped"
    const val STATUS_FAILED = "failed"
}

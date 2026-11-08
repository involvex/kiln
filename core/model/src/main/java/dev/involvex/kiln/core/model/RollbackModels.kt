package dev.involvex.kiln.core.model

import kotlinx.coroutines.flow.Flow

data class RollbackApkEntry(
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val apkAbsolutePath: String,
    val savedAtEpochMs: Long,
    val sizeBytes: Long,
)

/**
 * Local APK version cache for rollback. Implemented in `:feature:installer`.
 */
interface ApkRollbackCache {
    fun observe(packageName: String): Flow<List<RollbackApkEntry>>

    suspend fun list(packageName: String): List<RollbackApkEntry>

    /** Copy [apkAbsolutePath] into the cache after a successful install. */
    suspend fun rememberSuccessfulInstall(packageName: String, versionName: String, versionCode: Long, apkAbsolutePath: String)

    suspend fun get(packageName: String, versionCode: Long): RollbackApkEntry?
}

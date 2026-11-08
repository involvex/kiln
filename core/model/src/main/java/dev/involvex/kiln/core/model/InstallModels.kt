package dev.involvex.kiln.core.model

import dev.involvex.kiln.core.common.Result
import kotlinx.coroutines.flow.Flow

enum class InstallJobStatus {
    QUEUED,
    DOWNLOADING,
    VERIFYING,
    INSTALLING,
    SUCCEEDED,
    FAILED,
    CANCELLED,
}

data class InstallJobState(
    val jobId: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val status: InstallJobStatus,
    val progressPercent: Int = 0,
    val errorMessage: String? = null,
    val localApkPath: String? = null,
)

/**
 * Download + PackageInstaller queue (Tier NONE).
 * Implemented in `:feature:installer`, bound in `:app`.
 */
interface InstallQueue {
    fun jobs(): Flow<Map<String, InstallJobState>>

    suspend fun enqueue(update: AppUpdateInfo): Result<String>

    suspend fun cancel(jobId: String)
}

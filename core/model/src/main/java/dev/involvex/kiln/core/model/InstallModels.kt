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

    /**
     * Enqueue a download + install. For installed packages with a known signer mismatch
     * ([AppUpdateInfo.confidence] WARNING), pass [allowSignatureMismatch] after an explicit
     * user confirm. Auto-install / Update-all / automation must leave it false.
     */
    suspend fun enqueue(update: AppUpdateInfo, allowSignatureMismatch: Boolean = false, targetUserId: Int? = null): Result<String>

    /** Install a local APK file (backup restore). Skips network download. */
    suspend fun enqueueLocal(request: LocalInstallRequest): Result<String>

    suspend fun cancel(jobId: String)
}

/**
 * Local APK path(s) for restore / sideload without a download URL.
 * [apkAbsolutePaths] is ordered base-first; size ≥ 1. Single-APK callers pass a one-element list.
 */
data class LocalInstallRequest(
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val apkAbsolutePaths: List<String>,
    val expectedSha256: String? = null,
    /** Android `--user` for silent install (M55). Null = user 0. */
    val targetUserId: Int? = null,
) {
    init {
        require(apkAbsolutePaths.isNotEmpty()) { "apkAbsolutePaths must not be empty" }
    }

    /** Base / only APK path (backward-compatible accessor). */
    val apkAbsolutePath: String get() = apkAbsolutePaths.first()

    companion object {
        operator fun invoke(
            packageName: String,
            versionName: String,
            versionCode: Long,
            apkAbsolutePath: String,
            expectedSha256: String? = null,
            targetUserId: Int? = null,
        ): LocalInstallRequest = LocalInstallRequest(
            packageName = packageName,
            versionName = versionName,
            versionCode = versionCode,
            apkAbsolutePaths = listOf(apkAbsolutePath),
            expectedSha256 = expectedSha256,
            targetUserId = targetUserId,
        )
    }
}

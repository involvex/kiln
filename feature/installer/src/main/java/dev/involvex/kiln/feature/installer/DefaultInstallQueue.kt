package dev.involvex.kiln.feature.installer

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.core.database.InstallJobDao
import dev.involvex.kiln.core.database.InstallJobEntity
import dev.involvex.kiln.core.model.AppUpdateInfo
import dev.involvex.kiln.core.model.InstallJobState
import dev.involvex.kiln.core.model.InstallJobStatus
import dev.involvex.kiln.core.model.InstallQueue
import dev.involvex.kiln.core.model.InstalledAppsReader
import dev.involvex.kiln.core.model.LocalInstallRequest
import dev.involvex.kiln.feature.installer.work.InstallWorker
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class DefaultInstallQueue @Inject constructor(
    @ApplicationContext private val context: Context,
    private val installJobDao: InstallJobDao,
    private val installedAppsReader: InstalledAppsReader,
) : InstallQueue {
    override fun jobs(): Flow<Map<String, InstallJobState>> = installJobDao.observeAll().map { list ->
        list.associate { entity ->
            entity.packageName to entity.toState()
        }
    }

    override suspend fun enqueue(update: AppUpdateInfo): Result<String> {
        val url = update.downloadUrl?.takeIf { it.startsWith("https://") }
            ?: return Result.failure("missing_download_url")
        if (!update.signatureMatches) {
            val installed = installedAppsReader.userApps().any { it.packageName == update.packageName }
            if (installed) {
                return Result.failure("signature_mismatch")
            }
            // First-time install (e.g. GitHub discovery): worker verifies APK signature at install time.
        }
        return enqueueJob(
            packageName = update.packageName,
            versionName = update.availableVersionName,
            versionCode = update.availableVersionCode,
            downloadUrl = url,
            expectedSha256 = update.apkSha256,
            sizeBytes = update.sizeBytes,
            localApkPath = null,
            requiresGitHubAuth = update.requiresGitHubAuth,
            apkAssetRegex = update.apkAssetRegex,
        )
    }

    override suspend fun enqueueLocal(request: LocalInstallRequest): Result<String> {
        val apk = File(request.apkAbsolutePath)
        if (!apk.isFile || !apk.canRead()) {
            return Result.failure("apk_missing")
        }
        return enqueueJob(
            packageName = request.packageName,
            versionName = request.versionName,
            versionCode = request.versionCode,
            downloadUrl = LOCAL_URL_PREFIX,
            expectedSha256 = request.expectedSha256,
            sizeBytes = apk.length(),
            localApkPath = apk.absolutePath,
            requiresGitHubAuth = false,
            apkAssetRegex = null,
        )
    }

    private suspend fun enqueueJob(
        packageName: String,
        versionName: String,
        versionCode: Long,
        downloadUrl: String,
        expectedSha256: String?,
        sizeBytes: Long?,
        localApkPath: String?,
        requiresGitHubAuth: Boolean,
        apkAssetRegex: String?,
    ): Result<String> {
        val now = System.currentTimeMillis()
        val jobId = UUID.randomUUID().toString()
        installJobDao.upsert(
            InstallJobEntity(
                jobId = jobId,
                packageName = packageName,
                versionName = versionName,
                versionCode = versionCode,
                downloadUrl = downloadUrl,
                expectedSha256 = expectedSha256,
                sizeBytes = sizeBytes,
                status = InstallJobStatus.QUEUED.name,
                progressPercent = 0,
                errorMessage = null,
                localApkPath = localApkPath,
                requiresGitHubAuth = requiresGitHubAuth,
                apkAssetRegex = apkAssetRegex,
                createdAtEpochMs = now,
                updatedAtEpochMs = now,
            ),
        )
        val request = OneTimeWorkRequestBuilder<InstallWorker>()
            .setInputData(workDataOf(InstallWorker.KEY_JOB_ID to jobId))
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            workName(packageName),
            ExistingWorkPolicy.REPLACE,
            request,
        )
        return Result.success(jobId)
    }

    companion object {
        const val LOCAL_URL_PREFIX = "local:"
    }

    override suspend fun cancel(jobId: String) {
        val job = installJobDao.get(jobId) ?: return
        WorkManager.getInstance(context).cancelUniqueWork(workName(job.packageName))
        installJobDao.updateProgress(
            jobId = jobId,
            status = InstallJobStatus.CANCELLED.name,
            progressPercent = job.progressPercent,
            errorMessage = null,
            localApkPath = job.localApkPath,
            updatedAtEpochMs = System.currentTimeMillis(),
        )
    }

    private fun workName(packageName: String) = "install:$packageName"

    private fun InstallJobEntity.toState(): InstallJobState = InstallJobState(
        jobId = jobId,
        packageName = packageName,
        versionName = versionName,
        versionCode = versionCode,
        status = runCatching { InstallJobStatus.valueOf(status) }.getOrDefault(InstallJobStatus.FAILED),
        progressPercent = progressPercent,
        errorMessage = errorMessage,
        localApkPath = localApkPath,
    )
}

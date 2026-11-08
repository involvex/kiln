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
import dev.involvex.kiln.feature.installer.work.InstallWorker
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class DefaultInstallQueue @Inject constructor(@ApplicationContext private val context: Context, private val installJobDao: InstallJobDao) :
    InstallQueue {
    override fun jobs(): Flow<Map<String, InstallJobState>> = installJobDao.observeAll().map { list ->
        list.associate { entity ->
            entity.packageName to entity.toState()
        }
    }

    override suspend fun enqueue(update: AppUpdateInfo): Result<String> {
        val url = update.downloadUrl?.takeIf { it.startsWith("https://") }
            ?: return Result.failure("missing_download_url")
        if (!update.signatureMatches && update.apkSha256 != null) {
            // Still allow enqueue only when signatureMatches; WARNUNG updates are blocked in UI.
        }
        if (!update.signatureMatches) {
            return Result.failure("signature_mismatch")
        }
        val now = System.currentTimeMillis()
        val jobId = UUID.randomUUID().toString()
        installJobDao.upsert(
            InstallJobEntity(
                jobId = jobId,
                packageName = update.packageName,
                versionName = update.availableVersionName,
                versionCode = update.availableVersionCode,
                downloadUrl = url,
                expectedSha256 = update.apkSha256,
                sizeBytes = update.sizeBytes,
                status = InstallJobStatus.QUEUED.name,
                progressPercent = 0,
                errorMessage = null,
                localApkPath = null,
                createdAtEpochMs = now,
                updatedAtEpochMs = now,
            ),
        )
        val request = OneTimeWorkRequestBuilder<InstallWorker>()
            .setInputData(workDataOf(InstallWorker.KEY_JOB_ID to jobId))
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            workName(update.packageName),
            ExistingWorkPolicy.REPLACE,
            request,
        )
        return Result.success(jobId)
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

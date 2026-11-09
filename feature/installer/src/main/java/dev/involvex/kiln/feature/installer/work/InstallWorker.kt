package dev.involvex.kiln.feature.installer.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.involvex.kiln.core.common.Result as KilnResult
import dev.involvex.kiln.core.database.InstallJobDao
import dev.involvex.kiln.core.model.ApkRollbackCache
import dev.involvex.kiln.core.model.GitHubAuthHeaderProvider
import dev.involvex.kiln.core.model.InstallJobStatus
import dev.involvex.kiln.core.model.InstallerPreferences
import dev.involvex.kiln.core.model.PreferredInstaller
import dev.involvex.kiln.core.model.PrivilegeCapability
import dev.involvex.kiln.core.model.PrivilegedExecutor
import dev.involvex.kiln.core.network.KilnHttpClient
import dev.involvex.kiln.feature.installer.ArtifactZipExtractor
import dev.involvex.kiln.feature.installer.DefaultInstallQueue
import dev.involvex.kiln.feature.installer.InstallCommitPolicy
import dev.involvex.kiln.feature.installer.R
import dev.involvex.kiln.feature.installer.session.PackageInstallerSessionRunner
import dev.involvex.kiln.feature.installer.verify.ApkHashVerifier
import dev.involvex.kiln.feature.installer.verify.ApkSignatureGate
import java.io.File
import java.io.IOException
import timber.log.Timber

@HiltWorker
class InstallWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted params: WorkerParameters,
    private val installJobDao: InstallJobDao,
    private val http: KilnHttpClient,
    private val sessionRunner: PackageInstallerSessionRunner,
    private val privilegedExecutor: PrivilegedExecutor,
    private val rollbackCache: ApkRollbackCache,
    private val installerPreferences: InstallerPreferences,
    private val gitHubAuth: GitHubAuthHeaderProvider,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val jobId = inputData.getString(KEY_JOB_ID) ?: return Result.failure()
        val job = installJobDao.get(jobId) ?: return Result.failure()
        ensureChannel()
        setForeground(foregroundInfo(job.packageName, 0))

        mark(jobId, InstallJobStatus.DOWNLOADING, 0, null, job.localApkPath)
        val downloadDir = File(appContext.cacheDir, DOWNLOAD_DIR).apply { mkdirs() }
        val apkFile = File(downloadDir, "$jobId.apk")
        if (job.downloadUrl.startsWith(DefaultInstallQueue.LOCAL_URL_PREFIX)) {
            val source = job.localApkPath?.let { File(it) }
            if (source == null || !source.isFile) {
                mark(jobId, InstallJobStatus.FAILED, 100, "apk_missing", null)
                return Result.failure(workDataOf(KEY_ERROR to "apk_missing"))
            }
            try {
                source.copyTo(apkFile, overwrite = true)
            } catch (e: IOException) {
                mark(jobId, InstallJobStatus.FAILED, 100, e.message ?: "copy_failed", null)
                return Result.failure(workDataOf(KEY_ERROR to (e.message ?: "copy_failed")))
            }
        } else {
            val headers = if (job.requiresGitHubAuth) {
                val auth = gitHubAuth.bearerHeaders()
                if (auth.isEmpty()) {
                    mark(jobId, InstallJobStatus.FAILED, 100, "github_pat_required", null)
                    return Result.failure(workDataOf(KEY_ERROR to "github_pat_required"))
                }
                auth
            } else {
                emptyMap()
            }
            val isZip = ArtifactZipExtractor.looksLikeArtifactZip(job.downloadUrl, job.requiresGitHubAuth)
            if (isZip) {
                val zipFile = File(downloadDir, "$jobId.zip")
                when (val download = http.downloadToFile(job.downloadUrl, zipFile, headers)) {
                    is KilnResult.Failure -> {
                        mark(jobId, InstallJobStatus.FAILED, 100, download.message, null)
                        return Result.failure(workDataOf(KEY_ERROR to download.message))
                    }

                    is KilnResult.Success -> Unit
                }
                val regex = runCatching {
                    Regex(job.apkAssetRegex ?: ".*\\.apk$")
                }.getOrElse { Regex(".*\\.apk$") }
                when (val extracted = ArtifactZipExtractor.extractMatchingApk(zipFile, apkFile, regex)) {
                    is KilnResult.Failure -> {
                        zipFile.delete()
                        mark(jobId, InstallJobStatus.FAILED, 100, extracted.message, null)
                        return Result.failure(workDataOf(KEY_ERROR to extracted.message))
                    }

                    is KilnResult.Success -> zipFile.delete()
                }
            } else {
                when (val download = http.downloadToFile(job.downloadUrl, apkFile, headers)) {
                    is KilnResult.Failure -> {
                        mark(jobId, InstallJobStatus.FAILED, 100, download.message, null)
                        return Result.failure(workDataOf(KEY_ERROR to download.message))
                    }

                    is KilnResult.Success -> Unit
                }
            }
        }

        mark(jobId, InstallJobStatus.VERIFYING, 92, null, apkFile.absolutePath)
        when (val hash = ApkHashVerifier.verifySha256(apkFile, job.expectedSha256)) {
            is KilnResult.Failure -> {
                apkFile.delete()
                mark(jobId, InstallJobStatus.FAILED, 100, hash.message, null)
                return Result.failure(workDataOf(KEY_ERROR to hash.message))
            }

            is KilnResult.Success -> Unit
        }
        when (val sig = ApkSignatureGate.check(appContext.packageManager, job.packageName, apkFile)) {
            is KilnResult.Failure -> {
                apkFile.delete()
                mark(jobId, InstallJobStatus.FAILED, 100, sig.message, null)
                return Result.failure(workDataOf(KEY_ERROR to sig.message))
            }

            is KilnResult.Success -> Unit
        }

        mark(jobId, InstallJobStatus.INSTALLING, 95, null, apkFile.absolutePath)
        val preferred = installerPreferences.current()
        val canSilent = InstallCommitPolicy.useSilentInstall(
            preferred = preferred,
            hasSilentCapability = PrivilegeCapability.SILENT_INSTALL in
                privilegedExecutor.currentStatus().capabilities,
        )
        if (canSilent) {
            return when (val silent = privilegedExecutor.installApk(apkFile.absolutePath)) {
                is KilnResult.Failure -> {
                    Timber.w("Silent install failed (%s); falling back to PackageInstaller", silent.message)
                    commitWithPackageInstaller(jobId, job.packageName, apkFile)
                }

                is KilnResult.Success -> {
                    mark(jobId, InstallJobStatus.SUCCEEDED, 100, null, apkFile.absolutePath)
                    rollbackCache.rememberSuccessfulInstall(
                        packageName = job.packageName,
                        versionName = job.versionName,
                        versionCode = job.versionCode,
                        apkAbsolutePath = apkFile.absolutePath,
                    )
                    Result.success()
                }
            }
        }
        if (preferred == PreferredInstaller.SHIZUKU) {
            Timber.d("Shizuku preferred but SILENT_INSTALL unavailable; using PackageInstaller")
        }
        return commitWithPackageInstaller(jobId, job.packageName, apkFile)
    }

    private suspend fun commitWithPackageInstaller(jobId: String, packageName: String, apkFile: File): Result {
        if (!appContext.packageManager.canRequestPackageInstalls()) {
            mark(jobId, InstallJobStatus.FAILED, 100, "install_permission_required", apkFile.absolutePath)
            return Result.failure(workDataOf(KEY_ERROR to "install_permission_required"))
        }
        return when (val commit = sessionRunner.commitApk(appContext, jobId, packageName, apkFile)) {
            is KilnResult.Failure -> {
                mark(jobId, InstallJobStatus.FAILED, 100, commit.message, apkFile.absolutePath)
                Result.failure(workDataOf(KEY_ERROR to commit.message))
            }

            is KilnResult.Success -> Result.success()
        }
    }

    private suspend fun mark(jobId: String, status: InstallJobStatus, progress: Int, error: String?, path: String?) {
        installJobDao.updateProgress(
            jobId = jobId,
            status = status.name,
            progressPercent = progress,
            errorMessage = error,
            localApkPath = path,
            updatedAtEpochMs = System.currentTimeMillis(),
        )
        Timber.d("Install job %s → %s (%d)", jobId, status, progress)
    }

    private fun ensureChannel() {
        // minSdk 26: notification channels always available.
        val manager = appContext.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            appContext.getString(R.string.installer_notification_channel),
            NotificationManager.IMPORTANCE_LOW,
        )
        manager.createNotificationChannel(channel)
    }

    private fun foregroundInfo(packageName: String, progress: Int): ForegroundInfo {
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setContentTitle(appContext.getString(R.string.installer_notification_title))
            .setContentText(packageName)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, progress, progress == 0)
            .build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        const val KEY_JOB_ID = "job_id"
        const val KEY_ERROR = "error"
        private const val CHANNEL_ID = "kiln_install"
        private const val NOTIFICATION_ID = 4203
        private const val DOWNLOAD_DIR = "apk_downloads"
    }
}

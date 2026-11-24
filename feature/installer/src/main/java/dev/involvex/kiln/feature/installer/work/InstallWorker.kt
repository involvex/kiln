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
import dev.involvex.kiln.core.common.ApkSigningReader
import dev.involvex.kiln.core.common.RepoMirrorUrls
import dev.involvex.kiln.core.common.Result as KilnResult
import dev.involvex.kiln.core.database.InstallJobDao
import dev.involvex.kiln.core.database.RepoDao
import dev.involvex.kiln.core.model.ActivityLogEvents
import dev.involvex.kiln.core.model.ActivityLogRecorder
import dev.involvex.kiln.core.model.ActivityLogType
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
import dev.involvex.kiln.feature.installer.InstallPackageName
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
    private val repoDao: RepoDao,
    private val http: KilnHttpClient,
    private val sessionRunner: PackageInstallerSessionRunner,
    private val privilegedExecutor: PrivilegedExecutor,
    private val rollbackCache: ApkRollbackCache,
    private val installerPreferences: InstallerPreferences,
    private val gitHubAuth: GitHubAuthHeaderProvider,
    private val activityLog: ActivityLogRecorder,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val jobId = inputData.getString(KEY_JOB_ID) ?: return Result.failure()
        val job = installJobDao.get(jobId) ?: return Result.failure()
        ensureChannel()
        setForeground(foregroundInfo(job.packageName, 0))

        mark(jobId, InstallJobStatus.DOWNLOADING, 0, null, job.localApkPath)
        val downloadDir = File(appContext.cacheDir, DOWNLOAD_DIR).apply { mkdirs() }
        val apkFiles: List<File> = if (job.downloadUrl.startsWith(DefaultInstallQueue.LOCAL_URL_PREFIX)) {
            when (val prepared = prepareLocalApks(jobId, job.localApkPath, downloadDir)) {
                is KilnResult.Failure -> {
                    mark(jobId, InstallJobStatus.FAILED, 100, prepared.message, null)
                    return Result.failure(workDataOf(KEY_ERROR to prepared.message))
                }

                is KilnResult.Success -> prepared.value
            }
        } else {
            val apkFile = File(downloadDir, "$jobId.apk")
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
            val downloadUrls = resolveDownloadUrls(job.downloadUrl)
            if (isZip) {
                val zipFile = File(downloadDir, "$jobId.zip")
                when (val download = http.downloadToFileFirstSuccess(downloadUrls, zipFile, headers)) {
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
                when (val download = http.downloadToFileFirstSuccess(downloadUrls, apkFile, headers)) {
                    is KilnResult.Failure -> {
                        mark(jobId, InstallJobStatus.FAILED, 100, download.message, null)
                        return Result.failure(workDataOf(KEY_ERROR to download.message))
                    }

                    is KilnResult.Success -> Unit
                }
            }
            listOf(apkFile)
        }

        val baseApk = apkFiles.first()
        mark(jobId, InstallJobStatus.VERIFYING, 92, null, baseApk.absolutePath)
        when (val hash = ApkHashVerifier.verifySha256(baseApk, job.expectedSha256)) {
            is KilnResult.Failure -> {
                cleanupApks(apkFiles)
                mark(jobId, InstallJobStatus.FAILED, 100, hash.message, null)
                return Result.failure(workDataOf(KEY_ERROR to hash.message))
            }

            is KilnResult.Success -> Unit
        }
        // Discover GitHub uses provisional github.owner.repo ids; the APK's real id is required
        // for signature checks and PackageInstaller.setAppPackageName.
        val apkPackageName = ApkSigningReader.packageArchiveInfo(
            appContext.packageManager,
            baseApk.absolutePath,
        )?.packageName
        val installPackageName = InstallPackageName.resolve(job.packageName, apkPackageName)
        if (installPackageName != job.packageName) {
            Timber.d(
                "Install job %s: remapping package %s → %s from APK",
                jobId,
                job.packageName,
                installPackageName,
            )
        }
        if (!job.allowSignatureMismatch) {
            when (val sig = ApkSignatureGate.check(appContext.packageManager, installPackageName, baseApk)) {
                is KilnResult.Failure -> {
                    cleanupApks(apkFiles)
                    mark(jobId, InstallJobStatus.FAILED, 100, sig.message, null)
                    return Result.failure(workDataOf(KEY_ERROR to sig.message))
                }

                is KilnResult.Success -> Unit
            }
        }

        mark(jobId, InstallJobStatus.INSTALLING, 95, null, baseApk.absolutePath)
        val preferred = installerPreferences.current()
        val targetUserId = job.targetUserId ?: 0
        val canSilent = InstallCommitPolicy.useSilentInstall(
            preferred = preferred,
            hasSilentCapability = PrivilegeCapability.SILENT_INSTALL in
                privilegedExecutor.currentStatus().capabilities,
        )
        if (canSilent) {
            return when (
                val silent = privilegedExecutor.installApks(
                    apkAbsolutePaths = apkFiles.map { it.absolutePath },
                    targetUserId = targetUserId,
                )
            ) {
                is KilnResult.Failure -> {
                    // Foreign-user installs need silent pm; do not fall back to PackageInstaller (current user).
                    if (targetUserId != 0) {
                        Timber.w("Silent install for user %s failed: %s", targetUserId, silent.message)
                        cleanupApks(apkFiles)
                        mark(jobId, InstallJobStatus.FAILED, 100, silent.message, null)
                        return Result.failure(workDataOf(KEY_ERROR to silent.message))
                    }
                    Timber.w("Silent install failed (%s); falling back to PackageInstaller", silent.message)
                    commitWithPackageInstaller(jobId, installPackageName, apkFiles)
                }

                is KilnResult.Success -> {
                    mark(jobId, InstallJobStatus.SUCCEEDED, 100, null, baseApk.absolutePath)
                    if (apkFiles.size == 1) {
                        rollbackCache.rememberSuccessfulInstall(
                            packageName = installPackageName,
                            versionName = job.versionName,
                            versionCode = job.versionCode,
                            apkAbsolutePath = baseApk.absolutePath,
                        )
                    }
                    Result.success()
                }
            }
        }
        if (targetUserId != 0) {
            val msg = "silent_install_required_for_user"
            cleanupApks(apkFiles)
            mark(jobId, InstallJobStatus.FAILED, 100, msg, null)
            return Result.failure(workDataOf(KEY_ERROR to msg))
        }
        if (preferred == PreferredInstaller.SHIZUKU) {
            Timber.d("Shizuku preferred but SILENT_INSTALL unavailable; using PackageInstaller")
        }
        return commitWithPackageInstaller(jobId, installPackageName, apkFiles)
    }

    private fun prepareLocalApks(jobId: String, encodedPaths: String?, downloadDir: File): KilnResult<List<File>> {
        val sources = encodedPaths
            ?.split('\n')
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.map { File(it) }
            .orEmpty()
        if (sources.isEmpty() || sources.any { !it.isFile }) {
            return KilnResult.failure("apk_missing")
        }
        return try {
            if (sources.size == 1) {
                val apkFile = File(downloadDir, "$jobId.apk")
                sources.first().copyTo(apkFile, overwrite = true)
                KilnResult.success(listOf(apkFile))
            } else {
                val splitDir = File(downloadDir, jobId).apply { mkdirs() }
                val copied = sources.mapIndexed { index, source ->
                    val safe = source.name.replace(Regex("[^A-Za-z0-9._-]"), "_")
                        .ifBlank { "split-$index.apk" }
                    val dest = File(splitDir, safe)
                    source.copyTo(dest, overwrite = true)
                    dest
                }
                KilnResult.success(copied)
            }
        } catch (e: IOException) {
            KilnResult.failure(e.message ?: "copy_failed", e)
        }
    }

    private fun cleanupApks(apkFiles: List<File>) {
        apkFiles.forEach { it.delete() }
        apkFiles.firstOrNull()?.parentFile
            ?.takeIf { it.name != DOWNLOAD_DIR && it.isDirectory }
            ?.deleteRecursively()
    }

    private suspend fun commitWithPackageInstaller(jobId: String, packageName: String, apkFiles: List<File>): Result {
        val base = apkFiles.firstOrNull()
        if (base == null) {
            mark(jobId, InstallJobStatus.FAILED, 100, "apk_missing", null)
            return Result.failure(workDataOf(KEY_ERROR to "apk_missing"))
        }
        if (!appContext.packageManager.canRequestPackageInstalls()) {
            mark(jobId, InstallJobStatus.FAILED, 100, "install_permission_required", base.absolutePath)
            return Result.failure(workDataOf(KEY_ERROR to "install_permission_required"))
        }
        return when (val commit = sessionRunner.commitApks(appContext, jobId, packageName, apkFiles)) {
            is KilnResult.Failure -> {
                mark(jobId, InstallJobStatus.FAILED, 100, commit.message, base.absolutePath)
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
        if (status == InstallJobStatus.SUCCEEDED || status == InstallJobStatus.FAILED) {
            val job = installJobDao.get(jobId)
            activityLog.record(
                ActivityLogEvents.create(
                    type = ActivityLogType.INSTALL,
                    action = if (status == InstallJobStatus.SUCCEEDED) {
                        "install_succeeded"
                    } else {
                        "install_failed"
                    },
                    success = status == InstallJobStatus.SUCCEEDED,
                    packageName = job?.packageName,
                    versionName = job?.versionName,
                    versionCode = job?.versionCode,
                    detailKey = error,
                    refId = jobId,
                ),
            )
        }
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

    private suspend fun resolveDownloadUrls(primaryUrl: String): List<String> {
        val repos = repoDao.getAll().filter { it.kind == "FDROID" }
        val match = repos.firstOrNull { repo ->
            primaryUrl.startsWith(repo.address.trimEnd('/'))
        } ?: return listOf(primaryUrl)
        return RepoMirrorUrls.alternateDownloadUrls(primaryUrl, match.address, match.mirrorsCsv)
    }

    companion object {
        const val KEY_JOB_ID = "job_id"
        const val KEY_ERROR = "error"
        private const val CHANNEL_ID = "kiln_install"
        private const val NOTIFICATION_ID = 4203
        private const val DOWNLOAD_DIR = "apk_downloads"
    }
}

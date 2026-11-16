package dev.involvex.kiln.feature.apps.apktools

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.core.common.SplitApkSelector
import dev.involvex.kiln.core.model.ApkArchiveKind
import dev.involvex.kiln.core.model.InstallQueue
import dev.involvex.kiln.core.model.LocalInstallRequest
import java.io.File
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class InstallEnqueueInfo(val jobId: String, val packageName: String, val versionName: String, val splitCount: Int)

/**
 * Stages local APK / split-bundle files and enqueues [InstallQueue] jobs (Tier NONE path).
 */
@Singleton
class ApkInstallPreparer @Inject constructor(@ApplicationContext private val context: Context, private val installQueue: InstallQueue) {
    suspend fun enqueueFromCachedFile(cache: File, displayName: String): Result<InstallEnqueueInfo> =
        when (ApkArchiveReader.kindOf(displayName)) {
            ApkArchiveKind.APK -> enqueueSingleApk(cache, displayName)

            ApkArchiveKind.APKS,
            ApkArchiveKind.XAPK,
            ApkArchiveKind.APKM,
            -> enqueueBundle(cache, displayName)

            ApkArchiveKind.UNKNOWN -> Result.failure("unsupported_archive")
        }

    private suspend fun enqueueSingleApk(apk: File, displayName: String): Result<InstallEnqueueInfo> {
        val report = ApkArchiveReader.inspect(context.packageManager, apk, displayName)
        val packageName = report.packageName ?: return Result.failure("apk_unreadable")
        val versionCode = report.versionCode ?: 0L
        val versionName = report.versionName ?: versionCode.toString()
        val staging = File(context.cacheDir, "apk_tools/install-${System.currentTimeMillis()}").apply { mkdirs() }
        val staged = File(staging, "base.apk")
        apk.copyTo(staged, overwrite = true)
        return queueOrCleanup(staging, packageName, versionName, versionCode, listOf(staged.absolutePath))
    }

    private suspend fun enqueueBundle(bundle: File, displayName: String): Result<InstallEnqueueInfo> {
        val report = ApkArchiveReader.inspect(context.packageManager, bundle, displayName)
        val extractDir = File(context.cacheDir, "apk_tools/extract-${System.currentTimeMillis()}").apply { mkdirs() }
        try {
            val selected = selectSplitFiles(bundle, extractDir)
            if (selected is Result.Failure) return selected
            val selectedFiles = (selected as Result.Success).value
            val base = selectedFiles.first()
            val packageName = report.packageName ?: packageNameFromApk(base)
                ?: return Result.failure("apk_unreadable")
            val versionCode = report.versionCode ?: versionCodeFromApk(base) ?: 0L
            val versionName = report.versionName ?: versionNameFromApk(base) ?: versionCode.toString()
            val staging = File(context.cacheDir, "apk_tools/install-${System.currentTimeMillis()}").apply { mkdirs() }
            val stagedPaths = selectedFiles.mapIndexed { index, file ->
                val safe = file.name.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "split-$index.apk" }
                val dest = File(staging, safe)
                file.copyTo(dest, overwrite = true)
                dest.absolutePath
            }
            return queueOrCleanup(staging, packageName, versionName, versionCode, stagedPaths)
        } finally {
            extractDir.deleteRecursively()
        }
    }

    private fun selectSplitFiles(bundle: File, extractDir: File): Result<List<File>> {
        val extracted = ApkSplitExporter.extractApkMembers(bundle, extractDir)
        if (extracted.isFailure) {
            return Result.failure(extracted.exceptionOrNull()?.message ?: "bundle_extract_failed")
        }
        val memberFiles = extractDir.listFiles()?.filter { it.extension.equals("apk", ignoreCase = true) }.orEmpty()
        if (memberFiles.isEmpty()) {
            return Result.failure("bundle_no_apk")
        }
        val selection = SplitApkSelector.select(memberFiles.map { it.name }, deviceProfile())
        val selectionError = selection.errorCode
        if (selectionError != null) {
            return Result.failure(selectionError)
        }
        val selectedFiles = selection.members.mapNotNull { name ->
            memberFiles.firstOrNull { it.name.equals(name, ignoreCase = true) }
        }
        return if (selectedFiles.isEmpty()) {
            Result.failure("bundle_no_apk")
        } else {
            Result.success(selectedFiles)
        }
    }

    private suspend fun queueOrCleanup(
        staging: File,
        packageName: String,
        versionName: String,
        versionCode: Long,
        paths: List<String>,
    ): Result<InstallEnqueueInfo> = when (
        val queued = installQueue.enqueueLocal(
            LocalInstallRequest(
                packageName = packageName,
                versionName = versionName,
                versionCode = versionCode,
                apkAbsolutePaths = paths,
            ),
        )
    ) {
        is Result.Failure -> {
            staging.deleteRecursively()
            Result.failure(queued.message)
        }

        is Result.Success -> Result.success(
            InstallEnqueueInfo(
                jobId = queued.value,
                packageName = packageName,
                versionName = versionName,
                splitCount = paths.size,
            ),
        )
    }

    private fun deviceProfile(): SplitApkSelector.DeviceProfile {
        val abis = Build.SUPPORTED_ABIS.toList().ifEmpty { listOf("arm64-v8a") }
        val dpi = context.resources.displayMetrics.densityDpi
        val locales = buildList {
            val config = context.resources.configuration
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val list = config.locales
                for (i in 0 until list.size()) {
                    add(list[i].toLanguageTag())
                }
            } else {
                @Suppress("DEPRECATION")
                add(config.locale.toLanguageTag())
            }
            if (isEmpty()) add(Locale.getDefault().toLanguageTag())
        }
        return SplitApkSelector.DeviceProfile(abis = abis, densityDpi = dpi, languageTags = locales)
    }

    private fun packageNameFromApk(apk: File): String? = archivePackageInfo(apk)?.packageName

    private fun versionCodeFromApk(apk: File): Long? {
        val info = archivePackageInfo(apk) ?: return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }
    }

    private fun versionNameFromApk(apk: File): String? = archivePackageInfo(apk)?.versionName

    private fun archivePackageInfo(apk: File): android.content.pm.PackageInfo? {
        val flags = PackageManager.GET_META_DATA
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageArchiveInfo(
                apk.absolutePath,
                PackageManager.PackageInfoFlags.of(flags.toLong()),
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageArchiveInfo(apk.absolutePath, flags)
        }
    }
}

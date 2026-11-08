package dev.involvex.kiln.feature.backup.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.involvex.kiln.core.common.IoDispatcher
import dev.involvex.kiln.core.common.sha256CertificateFingerprint
import dev.involvex.kiln.core.model.BackupAppCandidate
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

data class BackupPackageDetail(val candidate: BackupAppCandidate, val apkFiles: List<File>, val signerSha256: List<String>)

@Singleton
class BackupPackageLoader @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    suspend fun listUserApps(): List<BackupAppCandidate> = withContext(ioDispatcher) {
        val pm = context.packageManager
        pm.getInstalledApplications(0)
            .asSequence()
            .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
            .mapNotNull { toCandidate(pm, it) }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    suspend fun loadDetail(packageName: String): BackupPackageDetail? = withContext(ioDispatcher) {
        val pm = context.packageManager
        val info = try {
            pm.getApplicationInfo(packageName, 0)
        } catch (_: PackageManager.NameNotFoundException) {
            return@withContext null
        }
        val candidate = toCandidate(pm, info) ?: return@withContext null
        val apkFiles = resolveApkFiles(info)
        BackupPackageDetail(
            candidate = candidate,
            apkFiles = apkFiles,
            signerSha256 = readSigners(pm, packageName),
        )
    }

    private fun toCandidate(pm: PackageManager, info: ApplicationInfo): BackupAppCandidate? {
        val pkgInfo = try {
            pm.getPackageInfo(info.packageName, 0)
        } catch (_: PackageManager.NameNotFoundException) {
            return null
        }
        val label = try {
            pm.getApplicationLabel(info).toString()
        } catch (_: Exception) {
            info.packageName
        }
        val apkFiles = resolveApkFiles(info)
        val size = apkFiles.sumOf { it.length() }
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pkgInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            pkgInfo.versionCode.toLong()
        }
        return BackupAppCandidate(
            packageName = info.packageName,
            label = label,
            versionName = pkgInfo.versionName,
            versionCode = versionCode,
            isSystem = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
            hasSplits = !info.splitSourceDirs.isNullOrEmpty(),
            apkSizeBytes = size,
        )
    }

    private fun resolveApkFiles(info: ApplicationInfo): List<File> {
        val paths = buildList {
            add(info.sourceDir)
            info.splitSourceDirs?.let { addAll(it) }
        }
        return paths.mapNotNull { path ->
            val file = File(path)
            file.takeIf { it.isFile && it.canRead() }
        }
    }

    private fun readSigners(pm: PackageManager, packageName: String): List<String> {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val info = pm.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                val signers = info.signingInfo?.apkContentsSigners ?: return emptyList()
                signers.map { sha256CertificateFingerprint(it.toByteArray()) }
            } else {
                @Suppress("DEPRECATION")
                val info = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
                @Suppress("DEPRECATION")
                info.signatures?.map { sha256CertificateFingerprint(it.toByteArray()) }.orEmpty()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}

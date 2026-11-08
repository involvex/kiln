package dev.involvex.kiln.feature.apps.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.involvex.kiln.core.model.ApkPathEntry
import dev.involvex.kiln.core.model.AppPermissionEntry
import dev.involvex.kiln.core.model.InstalledAppDetail
import dev.involvex.kiln.core.model.InstalledAppSummary
import java.io.File
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PackageManagerPackageInfoSource @Inject constructor(@ApplicationContext private val context: Context) : PackageInfoSource {
    private val packageManager: PackageManager = context.packageManager

    override suspend fun getInstalledSummaries(): List<InstalledAppSummary> {
        val packages = installedPackages(includeSignatures = false)
        return packages.mapNotNull { info -> toSummary(info) }
    }

    override suspend fun getDetail(packageName: String): InstalledAppDetail? {
        val info = packageInfoOrNull(packageName, includeSignatures = true) ?: return null
        return toDetail(info)
    }

    override fun isPackageInstalled(packageName: String): Boolean = try {
        packageManager.getPackageInfo(packageName, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    private fun installedPackages(includeSignatures: Boolean): List<PackageInfo> {
        // API 33+: PackageInfoFlags; older APIs use the int flag overload.
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(flags(includeSignatures).toLong()))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getInstalledPackages(flags(includeSignatures))
        }
    }

    private fun packageInfoOrNull(packageName: String, includeSignatures: Boolean): PackageInfo? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(
                packageName,
                PackageManager.PackageInfoFlags.of(flags(includeSignatures).toLong()),
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, flags(includeSignatures))
        }
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }

    private fun flags(includeSignatures: Boolean): Int {
        var value = 0
        if (includeSignatures) {
            // API 28+: signing certificates via GET_SIGNING_CERTIFICATES; API 26–27 use GET_SIGNATURES.
            value = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                value or PackageManager.GET_SIGNING_CERTIFICATES
            } else {
                @Suppress("DEPRECATION")
                value or PackageManager.GET_SIGNATURES
            }
        }
        value = value or PackageManager.GET_PERMISSIONS
        return value
    }

    private fun toSummary(info: PackageInfo): InstalledAppSummary? {
        val applicationInfo = info.applicationInfo ?: return null
        val label = applicationInfo.loadLabel(packageManager).toString()
        val paths = apkPaths(applicationInfo)
        val sizes = paths.map { it.sizeBytes }
        val apkSize = if (sizes.any { it == null }) null else sizes.sumOf { it ?: 0L }
        return InstalledAppSummary(
            packageName = info.packageName,
            label = label,
            versionName = info.versionName,
            versionCode = versionCodeOf(info),
            isSystem = applicationInfo.isSystemApp(),
            isUpdatedSystemApp = applicationInfo.isUpdatedSystemApp(),
            apkSizeBytes = apkSize,
            hasSplits = !applicationInfo.splitSourceDirs.isNullOrEmpty(),
            lastUpdateTime = info.lastUpdateTime,
            firstInstallTime = info.firstInstallTime,
        )
    }

    private fun toDetail(info: PackageInfo): InstalledAppDetail? {
        val applicationInfo = info.applicationInfo ?: return null
        val summary = toSummary(info) ?: return null
        val paths = apkPaths(applicationInfo)
        return InstalledAppDetail(
            packageName = summary.packageName,
            label = summary.label,
            versionName = summary.versionName,
            versionCode = summary.versionCode,
            isSystem = summary.isSystem,
            isUpdatedSystemApp = summary.isUpdatedSystemApp,
            apkSizeBytes = summary.apkSizeBytes,
            hasSplits = summary.hasSplits,
            lastUpdateTime = summary.lastUpdateTime,
            firstInstallTime = summary.firstInstallTime,
            installerPackage = installerPackageName(info.packageName),
            minSdk = applicationInfo.minSdkVersion,
            targetSdk = applicationInfo.targetSdkVersion,
            apkPaths = paths,
            signerSha256 = signerFingerprints(info),
            requestedPermissions = permissionsOf(info),
            hasLaunchIntent = packageManager.getLaunchIntentForPackage(info.packageName) != null,
        )
    }

    private fun apkPaths(applicationInfo: ApplicationInfo): List<ApkPathEntry> {
        val entries = mutableListOf<ApkPathEntry>()
        entries += ApkPathEntry(
            path = applicationInfo.sourceDir,
            sizeBytes = fileSizeOrNull(applicationInfo.sourceDir),
            splitId = null,
        )
        val splitDirs = applicationInfo.splitSourceDirs
        val splitNames = applicationInfo.splitNames
        if (splitDirs != null) {
            splitDirs.forEachIndexed { index, path ->
                val splitId = splitNames?.getOrNull(index) ?: "split_$index"
                entries += ApkPathEntry(
                    path = path,
                    sizeBytes = fileSizeOrNull(path),
                    splitId = splitId,
                )
            }
        }
        return entries
    }

    private fun fileSizeOrNull(path: String): Long? = try {
        val file = File(path)
        if (file.canRead()) file.length() else null
    } catch (_: SecurityException) {
        null
    }

    private fun versionCodeOf(info: PackageInfo): Long {
        // longVersionCode since API 28; int versionCode on API 26–27.
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }
    }

    private fun installerPackageName(packageName: String): String? {
        // API 30+: InstallSourceInfo; older getInstallerPackageName.
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                packageManager.getInstallSourceInfo(packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstallerPackageName(packageName)
            }
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }

    private fun signerFingerprints(info: PackageInfo): List<String> {
        val certificates = mutableListOf<ByteArray>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = info.signingInfo ?: return emptyList()
            val current = if (signingInfo.hasMultipleSigners()) {
                signingInfo.apkContentsSigners
            } else {
                signingInfo.signingCertificateHistory
            }
            current?.forEach { signature ->
                certificates += signature.toByteArray()
            }
        } else {
            @Suppress("DEPRECATION")
            info.signatures?.forEach { signature ->
                certificates += signature.toByteArray()
            }
        }
        return certificates.mapNotNull { bytes ->
            runCatching {
                val factory = CertificateFactory.getInstance("X.509")
                val cert = factory.generateCertificate(bytes.inputStream()) as X509Certificate
                sha256CertificateFingerprint(cert.encoded)
            }.getOrNull()
        }
    }

    private fun permissionsOf(info: PackageInfo): List<AppPermissionEntry> {
        val names = info.requestedPermissions ?: return emptyList()
        val flags = info.requestedPermissionsFlags
        return names.mapIndexed { index, name ->
            val granted = flags != null &&
                index < flags.size &&
                (flags[index] and PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0
            AppPermissionEntry(
                name = name,
                granted = granted,
                isDangerous = isDangerousPermission(name),
            )
        }
    }

    private fun isDangerousPermission(permission: String): Boolean = try {
        // Int-flag overload works on API 26–37; PermissionInfoFlags is API 33-only.
        @Suppress("DEPRECATION")
        val permissionInfo = packageManager.getPermissionInfo(permission, 0)
        // protection getter is API 28+; protectionLevel covers API 26–27.
        val level = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            permissionInfo.protection
        } else {
            @Suppress("DEPRECATION")
            permissionInfo.protectionLevel and PermissionInfo.PROTECTION_MASK_BASE
        }
        level == PermissionInfo.PROTECTION_DANGEROUS
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    private fun ApplicationInfo.isSystemApp(): Boolean = (flags and ApplicationInfo.FLAG_SYSTEM) != 0

    private fun ApplicationInfo.isUpdatedSystemApp(): Boolean = (flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
}

package dev.involvex.kiln.feature.apps.apktools

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import dev.involvex.kiln.core.common.ApkSigningReader
import dev.involvex.kiln.core.model.ApkArchiveKind
import dev.involvex.kiln.core.model.ApkInspectReport
import dev.involvex.kiln.core.model.ApkSignatureCompareReport
import java.io.File
import java.util.Locale
import java.util.zip.ZipFile

/**
 * Reads APK / APKS / XAPK archives for inspect + signature compare (Tier NONE).
 */
@Suppress("TooManyFunctions") // Inspect + compare + zip helpers share one entry type.
object ApkArchiveReader {
    fun kindOf(fileName: String): ApkArchiveKind {
        val lower = fileName.lowercase(Locale.US)
        return when {
            lower.endsWith(".apks") -> ApkArchiveKind.APKS
            lower.endsWith(".xapk") -> ApkArchiveKind.XAPK
            lower.endsWith(".apk") -> ApkArchiveKind.APK
            else -> ApkArchiveKind.UNKNOWN
        }
    }

    fun inspect(packageManager: PackageManager, file: File, displayName: String): ApkInspectReport {
        val kind = kindOf(displayName.ifBlank { file.name })
        return when (kind) {
            ApkArchiveKind.APK -> inspectSingleApk(packageManager, file, displayName, kind)

            ApkArchiveKind.APKS,
            ApkArchiveKind.XAPK,
            -> inspectBundle(packageManager, file, displayName, kind)

            ApkArchiveKind.UNKNOWN -> ApkInspectReport(
                displayName = displayName,
                kind = kind,
                packageName = null,
                versionName = null,
                versionCode = null,
                minSdk = null,
                targetSdk = null,
                permissions = emptyList(),
                signerSha256 = emptyList(),
                members = emptyList(),
                baseApkMember = null,
                errorCode = "unsupported_archive",
            )
        }
    }

    fun compareArchives(
        packageManager: PackageManager,
        left: File,
        leftLabel: String,
        right: File,
        rightLabel: String,
    ): ApkSignatureCompareReport {
        val leftSigners = signersFromFile(packageManager, left, leftLabel)
        val rightSigners = signersFromFile(packageManager, right, rightLabel)
        if (leftSigners.isEmpty() || rightSigners.isEmpty()) {
            return ApkSignatureCompareReport(
                leftLabel = leftLabel,
                rightLabel = rightLabel,
                leftSigners = leftSigners,
                rightSigners = rightSigners,
                matches = false,
                errorCode = "signer_unreadable",
            )
        }
        return ApkSignatureCompareReport(
            leftLabel = leftLabel,
            rightLabel = rightLabel,
            leftSigners = leftSigners,
            rightSigners = rightSigners,
            matches = ApkSigningReader.fingerprintsMatch(leftSigners, rightSigners),
        )
    }

    fun compareArchiveToInstalled(
        packageManager: PackageManager,
        apk: File,
        apkLabel: String,
        packageName: String,
    ): ApkSignatureCompareReport {
        val leftSigners = signersFromFile(packageManager, apk, apkLabel)
        val rightSigners = ApkSigningReader.fingerprintsFromInstalled(packageManager, packageName)
        if (leftSigners.isEmpty()) {
            return ApkSignatureCompareReport(
                leftLabel = apkLabel,
                rightLabel = packageName,
                leftSigners = leftSigners,
                rightSigners = rightSigners,
                matches = false,
                errorCode = "signer_unreadable",
            )
        }
        if (rightSigners.isEmpty()) {
            return ApkSignatureCompareReport(
                leftLabel = apkLabel,
                rightLabel = packageName,
                leftSigners = leftSigners,
                rightSigners = rightSigners,
                matches = false,
                errorCode = "package_not_installed",
            )
        }
        return ApkSignatureCompareReport(
            leftLabel = apkLabel,
            rightLabel = packageName,
            leftSigners = leftSigners,
            rightSigners = rightSigners,
            matches = ApkSigningReader.fingerprintsMatch(leftSigners, rightSigners),
        )
    }

    fun listZipMembers(file: File): List<String> = runCatching {
        ZipFile(file).use { zip ->
            zip.entries().asSequence()
                .filterNot { it.isDirectory }
                .map { it.name }
                .sorted()
                .toList()
        }
    }.getOrDefault(emptyList())

    fun pickBaseApkMember(members: List<String>): String? {
        val apkMembers = members.filter { it.lowercase(Locale.US).endsWith(".apk") }
        if (apkMembers.isEmpty()) return null
        return apkMembers.firstOrNull { it.substringAfterLast('/').equals("base.apk", ignoreCase = true) }
            ?: apkMembers.minByOrNull { it.length }
    }

    private fun signersFromFile(packageManager: PackageManager, file: File, label: String): List<String> {
        val kind = kindOf(label.ifBlank { file.name })
        return when (kind) {
            ApkArchiveKind.APK -> ApkSigningReader.fingerprintsFromArchive(packageManager, file)

            ApkArchiveKind.APKS,
            ApkArchiveKind.XAPK,
            -> {
                val members = listZipMembers(file)
                val base = pickBaseApkMember(members) ?: return emptyList()
                extractMemberToTemp(file, base)?.use { temp ->
                    ApkSigningReader.fingerprintsFromArchive(packageManager, temp)
                }.orEmpty()
            }

            ApkArchiveKind.UNKNOWN -> emptyList()
        }
    }

    private fun inspectSingleApk(packageManager: PackageManager, file: File, displayName: String, kind: ApkArchiveKind): ApkInspectReport {
        val info = ApkSigningReader.packageArchiveInfoWithPermissions(packageManager, file.absolutePath)
            ?: return ApkInspectReport(
                displayName = displayName,
                kind = kind,
                packageName = null,
                versionName = null,
                versionCode = null,
                minSdk = null,
                targetSdk = null,
                permissions = emptyList(),
                signerSha256 = emptyList(),
                members = listOf(file.name),
                baseApkMember = file.name,
                errorCode = "apk_unreadable",
            )
        return reportFromPackageInfo(displayName, kind, info, members = listOf(file.name), base = file.name)
    }

    private fun inspectBundle(packageManager: PackageManager, file: File, displayName: String, kind: ApkArchiveKind): ApkInspectReport {
        val members = listZipMembers(file)
        if (members.isEmpty()) {
            return ApkInspectReport(
                displayName = displayName,
                kind = kind,
                packageName = null,
                versionName = null,
                versionCode = null,
                minSdk = null,
                targetSdk = null,
                permissions = emptyList(),
                signerSha256 = emptyList(),
                members = emptyList(),
                baseApkMember = null,
                errorCode = "bundle_unreadable",
            )
        }
        val base = pickBaseApkMember(members)
        if (base == null) {
            return ApkInspectReport(
                displayName = displayName,
                kind = kind,
                packageName = null,
                versionName = null,
                versionCode = null,
                minSdk = null,
                targetSdk = null,
                permissions = emptyList(),
                signerSha256 = emptyList(),
                members = members,
                baseApkMember = null,
                errorCode = "bundle_no_apk",
            )
        }
        val temp = extractMemberToTemp(file, base)
            ?: return ApkInspectReport(
                displayName = displayName,
                kind = kind,
                packageName = null,
                versionName = null,
                versionCode = null,
                minSdk = null,
                targetSdk = null,
                permissions = emptyList(),
                signerSha256 = emptyList(),
                members = members,
                baseApkMember = base,
                errorCode = "bundle_extract_failed",
            )
        return temp.use { apk ->
            val info = ApkSigningReader.packageArchiveInfoWithPermissions(packageManager, apk.absolutePath)
            if (info == null) {
                ApkInspectReport(
                    displayName = displayName,
                    kind = kind,
                    packageName = null,
                    versionName = null,
                    versionCode = null,
                    minSdk = null,
                    targetSdk = null,
                    permissions = emptyList(),
                    signerSha256 = emptyList(),
                    members = members,
                    baseApkMember = base,
                    errorCode = "apk_unreadable",
                )
            } else {
                reportFromPackageInfo(displayName, kind, info, members, base)
            }
        }
    }

    private fun reportFromPackageInfo(
        displayName: String,
        kind: ApkArchiveKind,
        info: PackageInfo,
        members: List<String>,
        base: String?,
    ): ApkInspectReport {
        val appInfo = info.applicationInfo
        // minSdkVersion on ApplicationInfo: API 24+ (minSdk 26).
        val minSdk = appInfo?.minSdkVersion
        val targetSdk = appInfo?.targetSdkVersion
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }
        return ApkInspectReport(
            displayName = displayName,
            kind = kind,
            packageName = info.packageName,
            versionName = info.versionName,
            versionCode = versionCode,
            minSdk = minSdk,
            targetSdk = targetSdk,
            permissions = info.requestedPermissions?.toList().orEmpty(),
            signerSha256 = ApkSigningReader.fingerprintsFromPackageInfo(info),
            members = members,
            baseApkMember = base,
        )
    }

    private fun extractMemberToTemp(zipFile: File, memberName: String): File? = runCatching {
        ZipFile(zipFile).use { zip ->
            val entry = zip.getEntry(memberName) ?: return@runCatching null
            val out = File.createTempFile("kiln-apk-", ".apk")
            zip.getInputStream(entry).use { input ->
                out.outputStream().use { output -> input.copyTo(output) }
            }
            out
        }
    }.getOrNull()

    private inline fun <T> File.use(block: (File) -> T): T = try {
        block(this)
    } finally {
        delete()
    }
}

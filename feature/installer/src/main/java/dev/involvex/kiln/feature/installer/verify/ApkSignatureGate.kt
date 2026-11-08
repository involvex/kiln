package dev.involvex.kiln.feature.installer.verify

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.core.common.fingerprintsEqual
import dev.involvex.kiln.core.common.sha256CertificateFingerprint
import java.io.File

/**
 * Blocks install when the downloaded APK's signer does not match the installed app
 * (including signing certificate history on API 28+). New installs (package not present) pass.
 */
object ApkSignatureGate {
    fun check(packageManager: PackageManager, packageName: String, apkFile: File): Result<Unit> {
        val apkSigners = archiveSignerFingerprints(packageManager, apkFile)
        if (apkSigners.isEmpty()) {
            return Result.failure("apk_unsigned_or_unreadable")
        }
        val installedSigners = installedSignerFingerprints(packageManager, packageName)
        if (installedSigners.isEmpty()) {
            return Result.success(Unit)
        }
        val matches = apkSigners.any { apkFp ->
            installedSigners.any { fingerprintsEqual(apkFp, it) }
        }
        return if (matches) {
            Result.success(Unit)
        } else {
            Result.failure("signature_mismatch")
        }
    }

    private fun archiveSignerFingerprints(packageManager: PackageManager, apkFile: File): List<String> {
        val info = packageArchiveInfo(packageManager, apkFile.absolutePath) ?: return emptyList()
        return signerFingerprints(info)
    }

    private fun installedSignerFingerprints(packageManager: PackageManager, packageName: String): List<String> {
        return try {
            val info = packageInfo(packageManager, packageName) ?: return emptyList()
            signerFingerprints(info)
        } catch (_: PackageManager.NameNotFoundException) {
            emptyList()
        }
    }

    private fun packageArchiveInfo(packageManager: PackageManager, path: String): PackageInfo? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageArchiveInfo(
                path,
                PackageManager.PackageInfoFlags.of(signingFlags().toLong()),
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageArchiveInfo(path, signingFlags())
        }

    private fun packageInfo(packageManager: PackageManager, packageName: String): PackageInfo? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(
                packageName,
                PackageManager.PackageInfoFlags.of(signingFlags().toLong()),
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, signingFlags())
        }

    private fun signingFlags(): Int {
        // API 28+: SigningInfo; older devices use GET_SIGNATURES.
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }
    }

    private fun signerFingerprints(info: PackageInfo): List<String> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = info.signingInfo ?: return emptyList()
            val certs = if (signingInfo.hasMultipleSigners()) {
                signingInfo.apkContentsSigners
            } else {
                // Include history so key rotation still matches.
                signingInfo.signingCertificateHistory
            }
            return certs.orEmpty().map { sha256CertificateFingerprint(it.toByteArray()) }
        }
        @Suppress("DEPRECATION")
        return info.signatures.orEmpty().map { sha256CertificateFingerprint(it.toByteArray()) }
    }
}

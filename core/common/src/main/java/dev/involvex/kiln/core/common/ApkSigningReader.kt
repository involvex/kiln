package dev.involvex.kiln.core.common

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import java.io.File
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

/**
 * Shared APK / package signer fingerprints as SHA-256 of X.509 cert `encoded` bytes
 * (uppercase colon-hex), including signing-certificate history when available.
 */
object ApkSigningReader {
    fun signingFlags(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        PackageManager.GET_SIGNING_CERTIFICATES
    } else {
        @Suppress("DEPRECATION")
        PackageManager.GET_SIGNATURES
    }

    fun fingerprintsFromPackageInfo(info: PackageInfo): List<String> =
        signatureBytes(info).mapNotNull { bytes -> fingerprintFromSignatureBytes(bytes) }

    fun fingerprintsFromArchive(packageManager: PackageManager, apkFile: File): List<String> {
        val info = packageArchiveInfo(packageManager, apkFile.absolutePath) ?: return emptyList()
        return fingerprintsFromPackageInfo(info)
    }

    fun fingerprintsFromInstalled(packageManager: PackageManager, packageName: String): List<String> = try {
        val info = packageInfo(packageManager, packageName) ?: return emptyList()
        fingerprintsFromPackageInfo(info)
    } catch (_: PackageManager.NameNotFoundException) {
        emptyList()
    }

    fun packageArchiveInfo(packageManager: PackageManager, path: String): PackageInfo? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageArchiveInfo(
                path,
                PackageManager.PackageInfoFlags.of(signingFlags().toLong()),
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageArchiveInfo(path, signingFlags())
        }

    fun packageArchiveInfoWithPermissions(packageManager: PackageManager, path: String): PackageInfo? {
        val flags = signingFlags() or PackageManager.GET_PERMISSIONS
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageArchiveInfo(
                path,
                PackageManager.PackageInfoFlags.of(flags.toLong()),
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageArchiveInfo(path, flags)
        }
    }

    fun fingerprintsMatch(left: List<String>, right: List<String>): Boolean {
        if (left.isEmpty() || right.isEmpty()) return false
        return left.any { a -> right.any { b -> fingerprintsEqual(a, b) } }
    }

    fun fingerprintFromSignatureBytes(signatureBytes: ByteArray): String? = runCatching {
        val factory = CertificateFactory.getInstance("X.509")
        val cert = factory.generateCertificate(signatureBytes.inputStream()) as X509Certificate
        sha256CertificateFingerprint(cert.encoded)
    }.getOrNull()

    private fun packageInfo(packageManager: PackageManager, packageName: String): PackageInfo? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(
                packageName,
                PackageManager.PackageInfoFlags.of(signingFlags().toLong()),
            )
        } else {
            packageManager.getPackageInfo(packageName, signingFlags())
        }

    private fun signatureBytes(info: PackageInfo): List<ByteArray> {
        val out = mutableListOf<ByteArray>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = info.signingInfo ?: return emptyList()
            val certs: Array<Signature>? = if (signingInfo.hasMultipleSigners()) {
                signingInfo.apkContentsSigners
            } else {
                // Include history so key rotation still matches.
                signingInfo.signingCertificateHistory
            }
            certs?.forEach { signature -> out += signature.toByteArray() }
        } else {
            @Suppress("DEPRECATION")
            info.signatures?.forEach { signature -> out += signature.toByteArray() }
        }
        return out
    }
}

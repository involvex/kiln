package dev.involvex.kiln.feature.installer.verify

import android.content.pm.PackageManager
import dev.involvex.kiln.core.common.ApkSigningReader
import dev.involvex.kiln.core.common.Result
import java.io.File

/**
 * Blocks install when the downloaded APK's signer does not match the installed app
 * (including signing certificate history on API 28+). New installs (package not present) pass.
 */
object ApkSignatureGate {
    fun check(packageManager: PackageManager, packageName: String, apkFile: File): Result<Unit> {
        val apkSigners = ApkSigningReader.fingerprintsFromArchive(packageManager, apkFile)
        if (apkSigners.isEmpty()) {
            return Result.failure("apk_unsigned_or_unreadable")
        }
        val installedSigners = ApkSigningReader.fingerprintsFromInstalled(packageManager, packageName)
        if (installedSigners.isEmpty()) {
            return Result.success(Unit)
        }
        return if (ApkSigningReader.fingerprintsMatch(apkSigners, installedSigners)) {
            Result.success(Unit)
        } else {
            Result.failure("signature_mismatch")
        }
    }
}

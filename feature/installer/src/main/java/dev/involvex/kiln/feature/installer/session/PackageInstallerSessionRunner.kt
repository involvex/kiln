package dev.involvex.kiln.feature.installer.session

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.os.Build
import dev.involvex.kiln.core.common.Result
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class PackageInstallerSessionRunner @Inject constructor() {
    @SuppressLint("MissingPermission")
    fun commitApk(context: Context, jobId: String, packageName: String, apkFile: File): Result<Unit> {
        if (!apkFile.exists()) {
            return Result.failure("apk_missing")
        }
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(packageName)
            // API 34+: request update ownership for packages Kiln installs (REQUEST_INSTALL_PACKAGES path).
            // Lint flags ENFORCE_UPDATE_OWNERSHIP; that privileged permission is not required for
            // ordinary installers that already hold REQUEST_INSTALL_PACKAGES.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                setRequestUpdateOwnership(true)
            }
            // API 31+: skip confirm when Kiln already owns updates for this package.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && isKilnInstallerOfRecord(context, packageName)) {
                setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            }
        }
        return try {
            val sessionId = installer.createSession(params)
            installer.openSession(sessionId).use { session ->
                apkFile.inputStream().use { input ->
                    session.openWrite("base.apk", 0, apkFile.length()).use { output ->
                        input.copyTo(output)
                        session.fsync(output)
                    }
                }
                val callback = Intent(context, InstallStatusReceiver::class.java).apply {
                    action = InstallStatusReceiver.ACTION_INSTALL_STATUS
                    putExtra(InstallStatusReceiver.EXTRA_JOB_ID, jobId)
                    putExtra(InstallStatusReceiver.EXTRA_PACKAGE_NAME, packageName)
                }
                val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                val pending = PendingIntent.getBroadcast(context, sessionId, callback, flags)
                session.commit(pending.intentSender)
            }
            Result.success(Unit)
        } catch (error: IOException) {
            Timber.w(error, "PackageInstaller session failed")
            Result.failure(error.message ?: "session_io_failed", error)
        } catch (error: SecurityException) {
            Result.failure(error.message ?: "session_security_failed", error)
        }
    }

    private fun isKilnInstallerOfRecord(context: Context, packageName: String): Boolean = try {
        val installing = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.packageManager.getInstallSourceInfo(packageName).installingPackageName
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getInstallerPackageName(packageName)
        }
        installing == context.packageName
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }
}

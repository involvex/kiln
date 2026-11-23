package dev.involvex.kiln.feature.installer

/**
 * Discover GitHub installs often enqueue under a provisional id (`github.owner.repo`).
 * PackageInstaller must use the APK's real package name or the session fails / never finishes.
 */
object InstallPackageName {
    fun resolve(jobPackageName: String, apkPackageName: String?): String {
        val fromApk = apkPackageName?.trim().orEmpty()
        return fromApk.ifEmpty { jobPackageName }
    }

    fun isProvisional(packageName: String): Boolean = packageName.startsWith("github.")
}

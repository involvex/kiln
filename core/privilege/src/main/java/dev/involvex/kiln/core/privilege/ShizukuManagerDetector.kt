package dev.involvex.kiln.core.privilege

import android.content.Context
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class ShizukuManagerInfo(val packageName: String, val label: String, val versionName: String?)

/**
 * Detects whether Shizuku or ShizukuPlus is installed.
 * Does not imply the binder is alive or permission granted — that is [rikka.shizuku.Shizuku].
 */
@Singleton
class ShizukuManagerDetector @Inject constructor(@ApplicationContext private val context: Context) {
    fun detect(): ShizukuManagerInfo? {
        val pm = context.packageManager
        for (pkg in CANDIDATES) {
            val info = try {
                pm.getPackageInfo(pkg, 0)
            } catch (_: PackageManager.NameNotFoundException) {
                null
            } ?: continue
            val label = try {
                val appInfo = info.applicationInfo ?: pm.getApplicationInfo(pkg, 0)
                pm.getApplicationLabel(appInfo).toString()
            } catch (_: Exception) {
                pkg
            }
            return ShizukuManagerInfo(
                packageName = pkg,
                label = label,
                versionName = info.versionName,
            )
        }
        return null
    }

    fun launchIntent(packageName: String) = context.packageManager.getLaunchIntentForPackage(packageName)

    companion object {
        const val PACKAGE_SHIZUKU = "moe.shizuku.privileged.api"
        const val PACKAGE_SHIZUKU_PLUS = "af.shizuku.plus.api"

        val CANDIDATES = listOf(PACKAGE_SHIZUKU_PLUS, PACKAGE_SHIZUKU)
    }
}

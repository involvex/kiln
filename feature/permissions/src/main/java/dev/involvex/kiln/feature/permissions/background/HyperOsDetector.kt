package dev.involvex.kiln.feature.permissions.background

import android.os.Build
import timber.log.Timber

/**
 * Detects Xiaomi HyperOS / MIUI so Autostart AppOps (OEM-only) are shown.
 * Prefer `ro.miui.ui.version.name`; fall back to manufacturer brand.
 */
object HyperOsDetector {
    fun isHyperOsOrMiui(): Boolean {
        val miui = systemProperty("ro.miui.ui.version.name")
        if (!miui.isNullOrBlank()) return true
        val brand = Build.MANUFACTURER.lowercase()
        return brand == "xiaomi" || brand == "redmi" || brand == "poco"
    }

    private fun systemProperty(key: String): String? = try {
        val clazz = Class.forName("android.os.SystemProperties")
        val get = clazz.getMethod("get", String::class.java)
        (get.invoke(null, key) as? String)?.takeIf { it.isNotBlank() }
    } catch (error: ReflectiveOperationException) {
        Timber.d(error, "SystemProperties unavailable for %s", key)
        null
    }
}

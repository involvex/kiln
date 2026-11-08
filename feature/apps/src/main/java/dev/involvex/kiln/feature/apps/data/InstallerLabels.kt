package dev.involvex.kiln.feature.apps.data

/**
 * Small built-in map of known installer package names → display labels.
 * Not copied from any third-party string table.
 */
object InstallerLabels {
    private val known = mapOf(
        "com.android.vending" to "Google Play Store",
        "com.google.android.packageinstaller" to "Platform Package Installer",
        "com.android.packageinstaller" to "Platform Package Installer",
        "com.google.android.apps.nbu.files" to "Files by Google",
        "org.fdroid.fdroid" to "F-Droid",
        "org.fdroid.fdroid.privileged" to "F-Droid",
        "com.aurora.store" to "Aurora Store",
        "com.aurora.store.nightly" to "Aurora Store",
        "com.looker.droidify" to "Droid-ify",
        "com.machiav3lli.fdroid" to "Neo Store",
        "dev.imranr.obtainium" to "Obtainium",
        "dev.imranr.obtainium.fdroid" to "Obtainium",
    )

    fun displayName(installerPackage: String?): String? {
        if (installerPackage.isNullOrBlank()) return null
        return known[installerPackage] ?: installerPackage
    }
}

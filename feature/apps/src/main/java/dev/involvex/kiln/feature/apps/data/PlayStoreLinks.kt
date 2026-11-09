package dev.involvex.kiln.feature.apps.data

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Honest Play Store deep-links for unmanaged apps whose installer is Play or Aurora.
 * Does not verify that a Play listing exists.
 */
object PlayStoreLinks {
    private val playFamilyInstallers = setOf(
        "com.android.vending",
        "com.aurora.store",
        "com.aurora.store.nightly",
    )

    fun isPlayFamilyInstaller(installerPackage: String?): Boolean =
        !installerPackage.isNullOrBlank() && installerPackage in playFamilyInstallers

    fun shouldOfferOpenInPlayStore(hasManagedLink: Boolean, installerPackage: String?): Boolean =
        !hasManagedLink && isPlayFamilyInstaller(installerPackage)

    fun marketDetailsUrl(packageName: String): String = "market://details?id=$packageName"

    fun httpsDetailsUrl(packageName: String): String = "https://play.google.com/store/apps/details?id=$packageName"

    fun openListing(context: Context, packageName: String) {
        val market = Intent(Intent.ACTION_VIEW, Uri.parse(marketDetailsUrl(packageName)))
        try {
            context.startActivity(market)
        } catch (_: ActivityNotFoundException) {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(httpsDetailsUrl(packageName))),
            )
        }
    }
}

package dev.involvex.kiln.feature.apps.data

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PlayStoreLinksTest {
    @Test
    fun playAndAuroraInstallersAreRecognized() {
        assertTrue(PlayStoreLinks.isPlayFamilyInstaller("com.android.vending"))
        assertTrue(PlayStoreLinks.isPlayFamilyInstaller("com.aurora.store"))
        assertTrue(PlayStoreLinks.isPlayFamilyInstaller("com.aurora.store.nightly"))
        assertFalse(PlayStoreLinks.isPlayFamilyInstaller("org.fdroid.fdroid"))
        assertFalse(PlayStoreLinks.isPlayFamilyInstaller(null))
        assertFalse(PlayStoreLinks.isPlayFamilyInstaller(""))
    }

    @Test
    fun offerOnlyWhenUnmanagedAndPlayFamily() {
        assertTrue(
            PlayStoreLinks.shouldOfferOpenInPlayStore(
                hasManagedLink = false,
                installerPackage = "com.android.vending",
            ),
        )
        assertTrue(
            PlayStoreLinks.shouldOfferOpenInPlayStore(
                hasManagedLink = false,
                installerPackage = "com.aurora.store",
            ),
        )
        assertFalse(
            PlayStoreLinks.shouldOfferOpenInPlayStore(
                hasManagedLink = true,
                installerPackage = "com.android.vending",
            ),
        )
        assertFalse(
            PlayStoreLinks.shouldOfferOpenInPlayStore(
                hasManagedLink = false,
                installerPackage = "com.android.packageinstaller",
            ),
        )
    }

    @Test
    fun urlsUsePackageName() {
        val pkg = "com.example.app"
        assertEquals("market://details?id=$pkg", PlayStoreLinks.marketDetailsUrl(pkg))
        assertEquals(
            "https://play.google.com/store/apps/details?id=$pkg",
            PlayStoreLinks.httpsDetailsUrl(pkg),
        )
    }
}

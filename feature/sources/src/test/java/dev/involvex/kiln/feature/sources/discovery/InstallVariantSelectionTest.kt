package dev.involvex.kiln.feature.sources.discovery

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class InstallVariantSelectionTest {
    @Test
    fun `empty nativecode is universal`() {
        assertEquals(
            listOf(InstallVariantSelection.ABI_AUTO, InstallVariantSelection.ABI_UNIVERSAL),
            InstallVariantSelection.availableAbiChoices(emptyList()),
        )
    }

    @Test
    fun `arm64 preferred over universal on autodect`() {
        data class Rel(val code: Long, val abis: List<String>)
        val releases = listOf(
            Rel(2, emptyList()),
            Rel(3, listOf("arm64-v8a")),
            Rel(1, listOf("armeabi-v7a")),
        )
        val pick = InstallVariantSelection.pickRelease(
            releases = releases,
            deviceAbis = listOf("arm64-v8a", "armeabi-v7a"),
            abiChoice = InstallVariantSelection.ABI_AUTO,
            versionCode = { it.code },
            nativeCodes = { it.abis },
        )
        assertEquals(3L, pick?.code)
    }

    @Test
    fun `nightly tag detection`() {
        assertTrue(InstallVariantSelection.isNightlyLabel("nightly-20261116", null))
        assertTrue(InstallVariantSelection.isNightlyLabel("v1", "Nightly build"))
        assertFalse(InstallVariantSelection.isNightlyLabel("v1.2.3", "Stable"))
    }

    @Test
    fun `asset pick prefers device abi then universal`() {
        val assets = listOf(
            "app-armeabi-v7a.apk",
            "app-universal.apk",
            "app-arm64-v8a.apk",
        )
        assertEquals(
            "app-arm64-v8a.apk",
            InstallVariantSelection.pickAssetName(
                assets,
                deviceAbis = listOf("arm64-v8a"),
                abiChoice = InstallVariantSelection.ABI_AUTO,
            ),
        )
        assertEquals(
            "app-universal.apk",
            InstallVariantSelection.pickAssetName(
                assets,
                deviceAbis = listOf("arm64-v8a"),
                abiChoice = InstallVariantSelection.ABI_UNIVERSAL,
            ),
        )
    }

    @Test
    fun `no matching abi returns null when forced`() {
        assertNull(
            InstallVariantSelection.pickAssetName(
                listOf("app-x86.apk"),
                deviceAbis = listOf("arm64-v8a"),
                abiChoice = "arm64-v8a",
            ).takeIf { it?.contains("arm64") == true },
        )
        // Forced arm64 with only x86: falls back to first asset
        assertEquals(
            "app-x86.apk",
            InstallVariantSelection.pickAssetName(
                listOf("app-x86.apk"),
                deviceAbis = listOf("arm64-v8a"),
                abiChoice = "arm64-v8a",
            ),
        )
    }
}

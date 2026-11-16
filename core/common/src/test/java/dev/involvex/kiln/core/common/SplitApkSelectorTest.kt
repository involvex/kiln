package dev.involvex.kiln.core.common

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SplitApkSelectorTest {
    private val arm64De = SplitApkSelector.DeviceProfile(
        abis = listOf("arm64-v8a", "armeabi-v7a"),
        densityDpi = 440,
        languageTags = listOf("de-DE", "en"),
    )

    @Test
    fun selectsBaseAbiDensityAndMatchingLocale() {
        val members = listOf(
            "config.xxhdpi.apk",
            "base.apk",
            "config.armeabi_v7a.apk",
            "config.arm64_v8a.apk",
            "config.en.apk",
            "config.de.apk",
            "config.fr.apk",
        )
        val result = SplitApkSelector.select(members, arm64De)
        assertNull(result.errorCode)
        assertEquals(
            listOf(
                "base.apk",
                "config.arm64_v8a.apk",
                "config.xxhdpi.apk",
                "config.en.apk",
                "config.de.apk",
            ),
            result.members,
        )
        assertTrue(result.members.none { it.contains("fr") })
        assertTrue(result.members.none { it.contains("armeabi_v7a") })
    }

    @Test
    fun playStyleSplitConfigNames() {
        val members = listOf(
            "base.apk",
            "split_config.arm64_v8a.apk",
            "split_config.x86_64.apk",
            "split_config.xxhdpi.apk",
        )
        val result = SplitApkSelector.select(members, arm64De)
        assertNull(result.errorCode)
        assertTrue(result.members.contains("base.apk"))
        assertTrue(result.members.contains("split_config.arm64_v8a.apk"))
        assertTrue(result.members.contains("split_config.xxhdpi.apk"))
        assertTrue(result.members.none { it.contains("x86_64") })
    }

    @Test
    fun noCompatibleAbiFails() {
        val members = listOf("base.apk", "config.x86.apk", "config.x86_64.apk")
        val result = SplitApkSelector.select(members, arm64De)
        assertEquals("no_compatible_abi", result.errorCode)
        assertTrue(result.members.isEmpty())
    }

    @Test
    fun bundleWithoutApkFails() {
        val result = SplitApkSelector.select(listOf("manifest.json"), arm64De)
        assertEquals("bundle_no_apk", result.errorCode)
    }

    @Test
    fun featureSplitsIncluded() {
        val members = listOf("base.apk", "split_feature_foo.apk", "config.arm64_v8a.apk")
        val result = SplitApkSelector.select(members, arm64De)
        assertNull(result.errorCode)
        assertTrue(result.members.contains("split_feature_foo.apk"))
    }

    @Test
    fun singleBaseOnly() {
        val result = SplitApkSelector.select(listOf("base.apk"), arm64De)
        assertNull(result.errorCode)
        assertEquals(listOf("base.apk"), result.members)
    }
}

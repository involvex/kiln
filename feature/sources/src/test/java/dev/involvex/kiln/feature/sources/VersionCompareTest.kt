package dev.involvex.kiln.feature.sources

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VersionCompareTest {
    @Test
    fun `versionCode wins`() {
        assertTrue(isRemoteNewer(10, "1.0", 11, "0.9"))
        assertFalse(isRemoteNewer(11, "0.9", 10, "2.0"))
    }

    @Test
    fun `falls back to versionName`() {
        assertTrue(isRemoteNewer(0, "1.0.0", 0, "1.0.1"))
        assertFalse(isRemoteNewer(0, "2.0", 0, "1.9"))
    }
}

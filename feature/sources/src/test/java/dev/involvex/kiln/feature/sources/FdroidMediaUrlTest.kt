package dev.involvex.kiln.feature.sources

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class FdroidMediaUrlTest {
    @Test
    fun `joins repo and relative media path`() {
        assertEquals(
            "https://f-droid.org/repo/icons/icon.png",
            FdroidMediaUrl.join("https://f-droid.org/repo/", "/icons/icon.png"),
        )
    }

    @Test
    fun `returns null for blank path`() {
        assertNull(FdroidMediaUrl.join("https://example.com/repo", null))
        assertNull(FdroidMediaUrl.join("https://example.com/repo", "  "))
    }
}

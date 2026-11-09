package dev.involvex.kiln.core.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class KilnConfigFilenamesTest {
    @Test
    fun suggested_uses_utc_day() {
        // 2024-01-02T00:00:00Z
        val name = KilnConfigFilenames.suggested(1_704_153_600_000L)
        assertEquals("kiln-config_20240102.json", name)
        assertTrue(KilnConfigFilenames.MIME_JSON.contains("json"))
    }
}

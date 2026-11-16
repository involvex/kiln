package dev.involvex.kiln.core.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BackgroundUpdatePrefsTest {
    @Test
    fun sanitize_keeps_allowed_intervals() {
        assertEquals(12, BackgroundUpdatePrefs.sanitizeIntervalHours(12))
        assertEquals(24, BackgroundUpdatePrefs.sanitizeIntervalHours(24))
        assertEquals(168, BackgroundUpdatePrefs.sanitizeIntervalHours(168))
    }

    @Test
    fun sanitize_rejects_unknown_to_default() {
        assertEquals(
            BackgroundUpdatePrefs.DEFAULT_CHECK_INTERVAL_HOURS,
            BackgroundUpdatePrefs.sanitizeIntervalHours(6),
        )
        assertEquals(
            BackgroundUpdatePrefs.DEFAULT_CHECK_INTERVAL_HOURS,
            BackgroundUpdatePrefs.sanitizeIntervalHours(0),
        )
    }

    @Test
    fun sanitize_rollback_keep_clamps_to_1_5() {
        assertEquals(1, BackgroundUpdatePrefs.sanitizeRollbackKeepCount(0))
        assertEquals(3, BackgroundUpdatePrefs.sanitizeRollbackKeepCount(3))
        assertEquals(5, BackgroundUpdatePrefs.sanitizeRollbackKeepCount(9))
    }
}

package dev.involvex.kiln.core.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ActivityLogFilterTest {
    private val samples = listOf(
        ActivityLogEvents.create(
            type = ActivityLogType.INSTALL,
            action = "install_succeeded",
            success = true,
            packageName = "dev.involvex.kiln",
            detailKey = null,
            id = "1",
            epochMs = 1L,
        ),
        ActivityLogEvents.create(
            type = ActivityLogType.CACHE,
            action = "clear_cache",
            success = false,
            packageName = "com.example.app",
            detailKey = "capability_missing",
            id = "2",
            epochMs = 2L,
        ),
        ActivityLogEvents.create(
            type = ActivityLogType.SHELL,
            action = "pm_uninstall",
            success = true,
            packageName = "com.example.app",
            id = "3",
            epochMs = 3L,
        ),
    )

    @Test
    fun `empty query returns all`() {
        assertEquals(3, ActivityLogFilter.filter(samples, "").size)
    }

    @Test
    fun `filters by package substring`() {
        val result = ActivityLogFilter.filter(samples, "involvex")
        assertEquals(listOf("1"), result.map { it.id })
    }

    @Test
    fun `filters by type chip`() {
        val result = ActivityLogFilter.filter(samples, "", ActivityLogType.CACHE)
        assertEquals(listOf("2"), result.map { it.id })
    }

    @Test
    fun `filters by action and type together`() {
        val result = ActivityLogFilter.filter(samples, "pm_uninstall", ActivityLogType.SHELL)
        assertEquals(listOf("3"), result.map { it.id })
    }
}

class ActivityLogSanitizeTest {
    @Test
    fun `strips path-like detail keys`() {
        assertEquals("error", ActivityLogEvents.sanitizeDetailKey("error:/data/local/tmp/x.apk"))
        assertEquals("https", ActivityLogEvents.sanitizeDetailKey("https://evil.example/token"))
        assertEquals("capability_missing", ActivityLogEvents.sanitizeDetailKey("capability_missing"))
    }
}

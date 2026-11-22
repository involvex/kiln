package dev.involvex.kiln.core.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AppComponentFilterTest {
    @Test
    fun exportedOnly_keeps_exported_flags() {
        data class Row(val name: String, val exported: Boolean)
        val rows = listOf(Row("a", true), Row("b", false), Row("c", true))
        assertEquals(
            listOf("a", "c"),
            AppComponentFilter.exportedOnly(rows) { it.exported }.map { it.name },
        )
    }

    @Test
    fun matchesQuery_label_and_class() {
        assertTrue(AppComponentFilter.matchesQuery("Push Service", "com.app.PushService", ""))
        assertTrue(AppComponentFilter.matchesQuery("Push Service", "com.app.PushService", "push"))
        assertTrue(AppComponentFilter.matchesQuery("Push Service", "com.app.PushService", "PushService"))
        assertFalse(AppComponentFilter.matchesQuery("Push Service", "com.app.PushService", "debloat"))
    }
}

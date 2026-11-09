package dev.involvex.kiln.core.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WidgetUpdateCountTest {
    @Test
    fun `excludes watchOnly entries`() {
        val updates = mapOf(
            "a" to sample("a", watchOnly = false),
            "b" to sample("b", watchOnly = true),
            "c" to sample("c", watchOnly = false),
        )
        assertEquals(2, WidgetUpdateCount.count(updates))
    }

    @Test
    fun `empty map is zero`() {
        assertEquals(0, WidgetUpdateCount.count(emptyMap()))
    }

    private fun sample(packageName: String, watchOnly: Boolean) = AppUpdateInfo(
        packageName = packageName,
        availableVersionName = "1.0",
        availableVersionCode = 1L,
        sourceId = "test",
        sourceLabel = "Test",
        confidence = MatchConfidence.HIGH,
        signatureMatches = true,
        watchOnly = watchOnly,
    )
}

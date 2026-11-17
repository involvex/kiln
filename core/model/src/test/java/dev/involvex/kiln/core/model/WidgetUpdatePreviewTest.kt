package dev.involvex.kiln.core.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WidgetUpdatePreviewTest {
    @Test
    fun `excludes watchOnly and sorts by label`() {
        val updates = mapOf(
            "z.app" to sample("z.app", "2.0", watchOnly = false),
            "a.app" to sample("a.app", "1.1", watchOnly = false),
            "w.app" to sample("w.app", "9.0", watchOnly = true),
        )
        val lines = WidgetUpdatePreview.lines(
            updates = updates,
            labels = mapOf("z.app" to "Zulu", "a.app" to "Alpha"),
        )
        assertEquals(listOf("Alpha", "Zulu"), lines.map { it.label })
        assertEquals(listOf("1.1", "2.0"), lines.map { it.availableVersionName })
    }

    @Test
    fun `respects max lines`() {
        val updates = (1..10).associate { i ->
            "pkg.$i" to sample("pkg.$i", "1.$i", watchOnly = false)
        }
        assertEquals(5, WidgetUpdatePreview.lines(updates).size)
        assertEquals(3, WidgetUpdatePreview.lines(updates, limit = 3).size)
    }

    private fun sample(packageName: String, version: String, watchOnly: Boolean) = AppUpdateInfo(
        packageName = packageName,
        availableVersionName = version,
        availableVersionCode = 1L,
        sourceId = "test",
        sourceLabel = "Test",
        confidence = MatchConfidence.HIGH,
        signatureMatches = true,
        watchOnly = watchOnly,
    )
}

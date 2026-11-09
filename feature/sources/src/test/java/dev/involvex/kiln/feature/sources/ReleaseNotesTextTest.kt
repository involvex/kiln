package dev.involvex.kiln.feature.sources

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ReleaseNotesTextTest {
    @Test
    fun normalizeTrimsAndDropsBlank() {
        assertNull(ReleaseNotesText.normalize(null))
        assertNull(ReleaseNotesText.normalize("   "))
        assertEquals("hello", ReleaseNotesText.normalize("  hello  "))
    }

    @Test
    fun normalizeCapsLength() {
        val long = "a".repeat(ReleaseNotesText.MAX_STORE_CHARS + 50)
        val normalized = ReleaseNotesText.normalize(long)!!
        assertEquals(ReleaseNotesText.MAX_STORE_CHARS, normalized.length)
    }

    @Test
    fun previewEllipsizes() {
        val notes = "b".repeat(ReleaseNotesText.PREVIEW_CHARS + 20)
        val preview = ReleaseNotesText.preview(notes)
        assertTrue(preview.endsWith("…"))
        assertTrue(preview.length <= ReleaseNotesText.PREVIEW_CHARS + 1)
    }
}

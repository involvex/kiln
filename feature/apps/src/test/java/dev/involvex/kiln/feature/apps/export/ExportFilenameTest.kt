package dev.involvex.kiln.feature.apps.export

import dev.involvex.kiln.core.model.ApkExportFormat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ExportFilenameTest {
    @Test
    fun `builds sanitized filename with extension`() {
        val name = buildExportFilename(
            label = "My Cool App!",
            versionName = "1.2.3",
            versionCode = 42,
            format = ApkExportFormat.APKS,
        )
        assertEquals("My_Cool_App_1.2.3_42.apks", name)
    }

    @Test
    fun `sanitize strips path separators and limits length`() {
        val sanitized = sanitizeFilenamePart("../evil\\name:with*chars")
        assertFalse(sanitized.contains("/"))
        assertFalse(sanitized.contains("\\"))
        assertFalse(sanitized.contains(":"))
        assertTrue(sanitized.isNotBlank())
    }

    @Test
    fun `blank label falls back to app`() {
        val name = buildExportFilename(
            label = "@@@",
            versionName = null,
            versionCode = 1,
            format = ApkExportFormat.XAPK,
        )
        assertTrue(name.startsWith("app_"))
        assertTrue(name.endsWith(".xapk"))
    }
}

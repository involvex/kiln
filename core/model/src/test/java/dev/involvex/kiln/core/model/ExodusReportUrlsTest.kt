package dev.involvex.kiln.core.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ExodusReportUrlsTest {
    @Test
    fun reportPageUrl_usesLanguageAndPackage() {
        assertEquals(
            "https://reports.exodus-privacy.eu.org/en/reports/org.fdroid.fdroid/",
            ExodusReportUrls.reportPageUrl("org.fdroid.fdroid", "en-US"),
        )
        assertEquals(
            "https://reports.exodus-privacy.eu.org/de/reports/org.fdroid.fdroid/",
            ExodusReportUrls.reportPageUrl("org.fdroid.fdroid", "de-DE"),
        )
    }

    @Test
    fun reportPageUrl_rejectsBlank() {
        assertThrows(IllegalArgumentException::class.java) {
            ExodusReportUrls.reportPageUrl("  ")
        }
    }

    @Test
    fun normalizeApiBase_trimsSlash() {
        assertEquals(
            "https://exodus.example.com",
            ExodusReportUrls.normalizeApiBase("  https://exodus.example.com/  "),
        )
    }
}

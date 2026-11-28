package dev.involvex.kiln.core.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class WebDavUrlTest {
    @Test
    fun normalizeBaseUrl_rejectsHttp() {
        assertNull(WebDavUrl.normalizeBaseUrl("http://example.com/dav"))
        assertNull(WebDavUrl.normalizeBaseUrl("ftp://example.com"))
    }

    @Test
    fun normalizeBaseUrl_trimsTrailingSlash() {
        assertEquals(
            "https://cloud.example/remote.php/dav/files/user",
            WebDavUrl.normalizeBaseUrl("https://cloud.example/remote.php/dav/files/user/"),
        )
    }

    @Test
    fun join_encodesPackageAndFile() {
        val url = WebDavUrl.join(
            "https://cloud.example/dav",
            "/kiln-backups",
            "com.example.app",
            "com.example.app_1.0_1.kilnbackup.zip",
        )
        assertEquals(
            "https://cloud.example/dav/kiln-backups/com.example.app/com.example.app_1.0_1.kilnbackup.zip",
            url,
        )
    }

    @Test
    fun normalizeRemotePath_defaults() {
        assertEquals("/kiln-backups", WebDavUrl.normalizeRemotePath(""))
        assertEquals("/custom", WebDavUrl.normalizeRemotePath("custom/"))
    }
}

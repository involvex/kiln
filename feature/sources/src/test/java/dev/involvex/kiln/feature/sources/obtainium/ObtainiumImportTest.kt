package dev.involvex.kiln.feature.sources.obtainium

import dev.involvex.kiln.core.common.Result
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ObtainiumImportTest {
    @Test
    fun `parses keyed export with github url`() {
        val json = """
            {
              "com.example.app": {
                "id": "com.example.app",
                "url": "https://github.com/acme/widget"
              }
            }
        """.trimIndent()
        val result = ObtainiumImport.parse(json)
        assertTrue(result is Result.Success)
        val app = (result as Result.Success).value.single()
        assertEquals("com.example.app", app.packageName)
        assertEquals("acme/widget", app.githubOwnerRepo)
    }

    @Test
    fun `parses schemaVersion apps array with string additionalSettings`() {
        val json = """
            {
              "schemaVersion": 2,
              "exportedAt": "2026-01-01T00:00:00.000",
              "appVersion": "1.0.0",
              "apps": [
                {
                  "id": "org.fdroid.fdroid",
                  "url": "https://github.com/f-droid/fdroidclient",
                  "additionalSettings": "{\"apkFilterRegEx\":\".*\\\\.apk$\",\"includePrereleases\":false}"
                }
              ],
              "settings": null
            }
        """.trimIndent()
        val result = ObtainiumImport.parse(json)
        assertTrue(result is Result.Success, "parse failed: ${(result as? Result.Failure)?.message}")
        val app = (result as Result.Success).value.single()
        assertEquals("org.fdroid.fdroid", app.packageName)
        assertEquals("f-droid/fdroidclient", app.githubOwnerRepo)
        assertEquals("github:f-droid/fdroidclient", app.sourceId)
    }

    @Test
    fun `rejects truncated paste`() {
        val result = ObtainiumImport.parse("{")
        assertTrue(result is Result.Failure)
        assertEquals("obtainium_too_short", (result as Result.Failure).message)
    }

    @Test
    fun `rejects empty apps array`() {
        val result = ObtainiumImport.parse("""{"schemaVersion":2,"apps":[]}""")
        assertTrue(result is Result.Failure)
        assertEquals("obtainium_no_apps", (result as Result.Failure).message)
    }

    @Test
    fun `extracts owner repo from url`() {
        assertEquals(
            "acme/widget",
            ObtainiumImport.githubOwnerRepoFromUrl("https://github.com/acme/widget.git"),
        )
    }
}

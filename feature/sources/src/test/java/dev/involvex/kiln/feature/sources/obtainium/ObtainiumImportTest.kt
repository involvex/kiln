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
    fun `extracts owner repo from url`() {
        assertEquals(
            "acme/widget",
            ObtainiumImport.githubOwnerRepoFromUrl("https://github.com/acme/widget.git"),
        )
    }
}

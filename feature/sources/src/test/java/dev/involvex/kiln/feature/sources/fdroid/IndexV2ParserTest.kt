package dev.involvex.kiln.feature.sources.fdroid

import dev.involvex.kiln.core.common.Result
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class IndexV2ParserTest {
    @Test
    fun `parses entry pointer`() {
        val json = """{"index":{"name":"index-v2.json","sha256":"abc","size":12}}"""
        val result = IndexV2Parser.parseEntryPointer(json.toByteArray())
        assertTrue(result is Result.Success)
        val pointer = (result as Result.Success).value
        assertEquals("index-v2.json", pointer.name)
        assertEquals("abc", pointer.sha256)
    }

    @Test
    fun `parses package and release`() {
        val index = """
            {
              "packages": {
                "com.example.app": {
                  "metadata": {
                    "name": {"en-US": "Example"},
                    "summary": {"en-US": "Demo"},
                    "sourceCode": "https://github.com/example/app"
                  },
                  "versions": {
                    "deadbeef": {
                      "file": {"name": "app.apk", "sha256": "ff", "size": 10},
                      "manifest": {
                        "versionName": "1.2.3",
                        "versionCode": 123,
                        "signer": {"sha256": ["AA:BB"]}
                      }
                    }
                  }
                }
              }
            }
        """.trimIndent()
        val result = IndexV2Parser.parsePackages(index)
        assertTrue(result is Result.Success)
        val pkg = (result as Result.Success).value.single()
        assertEquals("com.example.app", pkg.packageName)
        assertEquals("Example", pkg.name)
        assertEquals(123L, pkg.suggestedVersionCode)
        assertEquals("AA:BB", pkg.releases.single().signerSha256.single())
    }
}

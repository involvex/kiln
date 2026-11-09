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
    fun `strips leading slash from index name`() {
        val json = """{"index":{"name":"/index-v2.json","sha256":"abc","size":12}}"""
        val result = IndexV2Parser.parseEntryPointer(json.toByteArray())
        assertTrue(result is Result.Success)
        assertEquals("index-v2.json", (result as Result.Success).value.name)
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
                    "sourceCode": "https://github.com/example/app",
                    "categories": ["Internet", "Security"]
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
        assertEquals(listOf("Internet", "Security"), pkg.categories)
        assertEquals(123L, pkg.suggestedVersionCode)
        assertEquals("AA:BB", pkg.releases.single().signerSha256.single())
    }

    @Test
    fun `parses antiFeatures map keys`() {
        val index = """
            {
              "packages": {
                "com.example.track": {
                  "metadata": {
                    "name": {"en-US": "Track"},
                    "antiFeatures": {
                      "Tracking": {"en-US": "Sends analytics"},
                      "NonFreeNet": {}
                    }
                  },
                  "versions": {}
                }
              }
            }
        """.trimIndent()
        val result = IndexV2Parser.parsePackages(index)
        assertTrue(result is Result.Success)
        val pkg = (result as Result.Success).value.single()
        assertEquals(listOf("NonFreeNet", "Tracking"), pkg.antiFeatures)
    }

    @Test
    fun `parses icon and phone screenshots paths`() {
        val index = """
            {
              "packages": {
                "com.example.media": {
                  "metadata": {
                    "name": {"en-US": "Media"},
                    "icon": {"en-US": {"name": "/icons/icon.png"}},
                    "phoneScreenshots": [
                      {"en-US": {"name": "phone/ss1.png"}},
                      {"name": "phone/ss2.png"}
                    ]
                  },
                  "versions": {}
                }
              }
            }
        """.trimIndent()
        val result = IndexV2Parser.parsePackages(index)
        assertTrue(result is Result.Success)
        val pkg = (result as Result.Success).value.single()
        assertEquals("icons/icon.png", pkg.iconPath?.trimStart('/'))
        assertEquals(listOf("phone/ss1.png", "phone/ss2.png"), pkg.phoneScreenshotPaths)
    }
}

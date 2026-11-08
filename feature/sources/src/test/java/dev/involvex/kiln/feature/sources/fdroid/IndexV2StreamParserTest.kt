package dev.involvex.kiln.feature.sources.fdroid

import dev.involvex.kiln.core.common.Result
import java.io.ByteArrayInputStream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class IndexV2StreamParserTest {
    @Test
    fun `keeps only wanted packages and skips the rest`() {
        val index = """
            {
              "repo": {"name": "Test"},
              "packages": {
                "com.skip.one": {
                  "metadata": {"name": {"en-US": "Skip1"}},
                  "versions": {
                    "a": {
                      "file": {"name": "a.apk", "sha256": "aa", "size": 1},
                      "manifest": {"versionName": "1", "versionCode": 1, "signer": {"sha256": ["AA"]}}
                    }
                  }
                },
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
                },
                "com.skip.two": {
                  "metadata": {"name": {"en-US": "Skip2"}},
                  "versions": {}
                }
              }
            }
        """.trimIndent()

        val result = IndexV2StreamParser.parseWantedPackages(
            ByteArrayInputStream(index.toByteArray()),
            setOf("com.example.app", "com.missing.app"),
        )
        assertTrue(result is Result.Success)
        val pkgs = (result as Result.Success).value
        assertEquals(1, pkgs.size)
        assertEquals("com.example.app", pkgs.single().packageName)
        assertEquals("Example", pkgs.single().name)
        assertEquals(123L, pkgs.single().suggestedVersionCode)
    }

    @Test
    fun `empty wanted set returns empty without failing`() {
        val index = """{"packages":{"com.example.app":{"metadata":{},"versions":{}}}}"""
        val result = IndexV2StreamParser.parseWantedPackages(
            ByteArrayInputStream(index.toByteArray()),
            emptySet(),
        )
        assertTrue(result is Result.Success)
        assertTrue((result as Result.Success).value.isEmpty())
    }
}

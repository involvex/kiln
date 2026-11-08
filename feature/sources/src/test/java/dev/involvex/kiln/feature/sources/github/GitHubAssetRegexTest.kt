package dev.involvex.kiln.feature.sources.github

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class GitHubAssetRegexTest {
    @Test
    fun `picks first asset matching regex preferring arm64`() {
        val assets = listOf(
            "app-armeabi-v7a-release.apk",
            "app-arm64-v8a-release.apk",
            "app-x86_64-release.apk",
        )
        val regex = Regex(".*arm64-v8a.*\\.apk$")
        val pick = assets.firstOrNull { regex.containsMatchIn(it) }
        assertEquals("app-arm64-v8a-release.apk", pick)
    }

    @Test
    fun `default apk regex skips non-apk`() {
        val assets = listOf("checksums.txt", "app.apk", "notes.md")
        val regex = Regex(".*\\.apk$")
        val pick = assets.firstOrNull { regex.containsMatchIn(it) }
        assertEquals("app.apk", pick)
    }

    @Test
    fun `returns null when nothing matches`() {
        val assets = listOf("source.tar.gz", "notes.md")
        val regex = Regex(".*\\.apk$")
        assertNull(assets.firstOrNull { regex.containsMatchIn(it) })
    }
}

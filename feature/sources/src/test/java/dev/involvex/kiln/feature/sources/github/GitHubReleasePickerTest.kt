package dev.involvex.kiln.feature.sources.github

import dev.involvex.kiln.core.model.GitHubReleaseChannel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class GitHubReleasePickerTest {
    private val apk = Regex(".*\\.apk$")

    @Test
    fun `stable skips prerelease`() {
        val releases = listOf(
            GitHubReleaseDto(
                tagName = "v2.0.0-beta",
                prerelease = true,
                assets = listOf(GitHubAssetDto("app.apk", "https://example.com/b.apk", 10)),
            ),
            GitHubReleaseDto(
                tagName = "v1.0.0",
                body = "  Bug fixes  ",
                prerelease = false,
                assets = listOf(GitHubAssetDto("app.apk", "https://example.com/s.apk", 20)),
            ),
        )
        val pick = GitHubReleasePicker.pick(releases, apk, GitHubReleaseChannel.STABLE)
        assertEquals("v1.0.0", pick?.tagName)
        assertEquals("https://example.com/s.apk", pick?.downloadUrl)
        assertEquals("Bug fixes", pick?.releaseNotes)
    }

    @Test
    fun `pre-release prefers first matching including prerelease`() {
        val releases = listOf(
            GitHubReleaseDto(
                tagName = "v2.0.0-beta",
                prerelease = true,
                assets = listOf(GitHubAssetDto("app.apk", "https://example.com/b.apk", 10)),
            ),
            GitHubReleaseDto(
                tagName = "v1.0.0",
                prerelease = false,
                assets = listOf(GitHubAssetDto("app.apk", "https://example.com/s.apk", 20)),
            ),
        )
        val pick = GitHubReleasePicker.pick(releases, apk, GitHubReleaseChannel.PRE_RELEASE)
        assertEquals("v2.0.0-beta", pick?.tagName)
    }

    @Test
    fun `no matching asset returns null`() {
        val releases = listOf(
            GitHubReleaseDto(
                tagName = "v1.0.0",
                assets = listOf(GitHubAssetDto("notes.txt", "https://example.com/n.txt", 1)),
            ),
        )
        assertNull(GitHubReleasePicker.pick(releases, apk, GitHubReleaseChannel.STABLE))
    }
}

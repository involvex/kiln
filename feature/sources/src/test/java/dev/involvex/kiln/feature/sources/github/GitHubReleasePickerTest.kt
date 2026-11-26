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

    @Test
    fun `prefers device abi asset when several apks match`() {
        val releases = listOf(
            GitHubReleaseDto(
                tagName = "v158.0.8127.42",
                assets = listOf(
                    GitHubAssetDto(
                        "158.0.8127.42-1-armeabi-v7a.apk",
                        "https://example.com/v7a.apk",
                        10,
                    ),
                    GitHubAssetDto(
                        "158.0.8127.42-1-arm64-v8a.apk",
                        "https://example.com/v8a.apk",
                        20,
                    ),
                ),
            ),
        )
        val pick = GitHubReleasePicker.pick(
            releases = releases,
            assetRegex = apk,
            channel = GitHubReleaseChannel.STABLE,
            preferredAbis = listOf("arm64-v8a", "armeabi-v7a"),
        )
        assertEquals("158.0.8127.42-1-arm64-v8a.apk", pick?.assetName)
        assertEquals("https://example.com/v8a.apk", pick?.downloadUrl)
    }

    @Test
    fun `preferAbiAsset falls back to first match`() {
        val assets = listOf(
            GitHubAssetDto("app-universal.apk", "https://example.com/u.apk", 1),
            GitHubAssetDto("app-x86.apk", "https://example.com/x86.apk", 2),
        )
        val chosen = GitHubReleasePicker.preferAbiAsset(assets, listOf("arm64-v8a"))
        assertEquals("app-universal.apk", chosen?.name)
    }

    @Test
    fun `match without fallback skips prerelease-only`() {
        val releases = listOf(
            GitHubReleaseDto(
                tagName = "v1.0.0-rc1",
                prerelease = true,
                assets = listOf(GitHubAssetDto("app.apk", "https://example.com/rc.apk", 10)),
            ),
        )
        assertNull(
            GitHubReleasePicker.match(
                releases = releases,
                assetRegex = apk,
                preferred = GitHubReleaseChannel.STABLE,
                fallbackToPreRelease = false,
            ),
        )
    }

    @Test
    fun `match with fallback uses pre-release when only prereleases have apk`() {
        val releases = listOf(
            GitHubReleaseDto(
                tagName = "v1.0.0-rc1",
                prerelease = true,
                assets = listOf(GitHubAssetDto("app.apk", "https://example.com/rc.apk", 10)),
            ),
        )
        val match = GitHubReleasePicker.match(
            releases = releases,
            assetRegex = apk,
            preferred = GitHubReleaseChannel.STABLE,
            fallbackToPreRelease = true,
        )
        assertEquals("v1.0.0-rc1", match?.pick?.tagName)
        assertEquals(GitHubReleaseChannel.PRE_RELEASE, match?.channel)
    }

    @Test
    fun `match with fallback still prefers stable when present`() {
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
        val match = GitHubReleasePicker.match(
            releases = releases,
            assetRegex = apk,
            preferred = GitHubReleaseChannel.STABLE,
            fallbackToPreRelease = true,
        )
        assertEquals("v1.0.0", match?.pick?.tagName)
        assertEquals(GitHubReleaseChannel.STABLE, match?.channel)
    }
}

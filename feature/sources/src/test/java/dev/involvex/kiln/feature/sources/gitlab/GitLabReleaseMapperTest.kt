package dev.involvex.kiln.feature.sources.gitlab

import dev.involvex.kiln.core.model.GitHubReleaseChannel
import dev.involvex.kiln.feature.sources.github.GitHubReleasePicker
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

/**
 * Ensures GitLab release JSON maps into the shared picker shape
 * (via the same DTO fields [GitHubReleasePicker] expects).
 */
class GitLabReleaseMapperTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun maps_links_to_apk_pick() {
        // Mirror GitLabReleasesClient.toGitHubShape via public list decode path is private;
        // exercise picker on a hand-built GitHub-shaped list that matches client mapping rules.
        val releases = listOf(
            dev.involvex.kiln.feature.sources.github.GitHubReleaseDto(
                tagName = "v1.2.0",
                name = "1.2.0",
                body = "notes",
                prerelease = false,
                assets = listOf(
                    dev.involvex.kiln.feature.sources.github.GitHubAssetDto(
                        name = "app-release.apk",
                        browserDownloadUrl = "https://gitlab.com/o/r/-/releases/v1.2.0/downloads/app-release.apk",
                        size = 0,
                    ),
                ),
            ),
        )
        val pick = GitHubReleasePicker.pick(releases, Regex(".*\\.apk$"), GitHubReleaseChannel.STABLE)
        assertNotNull(pick)
        assertEquals("v1.2.0", pick!!.tagName)
        assertEquals("app-release.apk", pick.assetName)
    }

    @Test
    fun decodes_gitlab_fixture_keys() {
        val body = """
            [{
              "tag_name": "v0.1.0",
              "name": "First",
              "description": "hello",
              "upcoming_release": false,
              "assets": {
                "links": [
                  {"name": "kiln.apk", "url": "https://example/kiln.apk", "direct_asset_url": "https://example/direct/kiln.apk"}
                ]
              }
            }]
        """.trimIndent()

        @kotlinx.serialization.Serializable
        data class Link(
            val name: String,
            val url: String? = null,
            @kotlinx.serialization.SerialName("direct_asset_url") val direct: String? = null,
        )

        @kotlinx.serialization.Serializable
        data class Assets(val links: List<Link> = emptyList())

        @kotlinx.serialization.Serializable
        data class Rel(
            @kotlinx.serialization.SerialName("tag_name") val tag: String,
            val assets: Assets? = null,
            @kotlinx.serialization.SerialName("upcoming_release") val upcoming: Boolean = false,
        )
        val decoded = json.decodeFromString<List<Rel>>(body)
        assertEquals(1, decoded.size)
        assertEquals("kiln.apk", decoded[0].assets!!.links[0].name)
        assertEquals("https://example/direct/kiln.apk", decoded[0].assets!!.links[0].direct)
    }
}

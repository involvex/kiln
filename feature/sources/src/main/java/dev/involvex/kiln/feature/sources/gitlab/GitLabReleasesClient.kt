package dev.involvex.kiln.feature.sources.gitlab

import android.os.Build
import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.core.model.GitHubReleaseChannel
import dev.involvex.kiln.core.model.ReleaseForge
import dev.involvex.kiln.core.network.KilnHttpClient
import dev.involvex.kiln.feature.sources.ReleaseNotesText
import dev.involvex.kiln.feature.sources.forge.ForgeTokenStore
import dev.involvex.kiln.feature.sources.github.GitHubAssetDto
import dev.involvex.kiln.feature.sources.github.GitHubReleaseDto
import dev.involvex.kiln.feature.sources.github.GitHubReleaseMatch
import dev.involvex.kiln.feature.sources.github.GitHubReleasePick
import dev.involvex.kiln.feature.sources.github.GitHubReleasePicker
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

@Serializable
private data class GitLabReleaseDto(
    @SerialName("tag_name") val tagName: String,
    val name: String? = null,
    val description: String? = null,
    @SerialName("upcoming_release") val upcomingRelease: Boolean = false,
    val assets: GitLabAssetsDto? = null,
)

@Serializable
private data class GitLabAssetsDto(val links: List<GitLabAssetLinkDto> = emptyList())

@Serializable
private data class GitLabAssetLinkDto(
    val name: String,
    val url: String? = null,
    @SerialName("direct_asset_url") val directAssetUrl: String? = null,
)

@Singleton
class GitLabReleasesClient @Inject constructor(private val http: KilnHttpClient, private val tokenStore: ForgeTokenStore) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun matchLatestRelease(
        ownerRepo: String,
        assetRegex: Regex,
        channel: GitHubReleaseChannel,
        fallbackToPreRelease: Boolean = false,
    ): Result<GitHubReleaseMatch> = when (val listed = listReleases(ownerRepo)) {
        is Result.Failure -> listed

        is Result.Success -> {
            val match = GitHubReleasePicker.match(
                releases = listed.value,
                assetRegex = assetRegex,
                preferred = channel,
                preferredAbis = Build.SUPPORTED_ABIS.toList(),
                fallbackToPreRelease = fallbackToPreRelease,
            )
            if (match == null) {
                Result.failure("no_matching_gitlab_asset")
            } else {
                Result.success(match)
            }
        }
    }

    suspend fun latestMatchingRelease(
        ownerRepo: String,
        assetRegex: Regex,
        channel: GitHubReleaseChannel,
        fallbackToPreRelease: Boolean = false,
    ): Result<GitHubReleasePick> = when (
        val matched = matchLatestRelease(ownerRepo, assetRegex, channel, fallbackToPreRelease)
    ) {
        is Result.Failure -> matched
        is Result.Success -> Result.success(matched.value.pick)
    }

    suspend fun listReleases(ownerRepo: String, perPage: Int = 20): Result<List<GitHubReleaseDto>> {
        val encoded = URLEncoder.encode(ownerRepo, StandardCharsets.UTF_8)
        val url = "https://gitlab.com/api/v4/projects/$encoded/releases?per_page=$perPage"
        val headers = buildMap {
            put("Accept", "application/json")
            tokenStore.getToken(ReleaseForge.GITLAB)?.let { put("PRIVATE-TOKEN", it) }
        }
        val body = when (val r = http.getText(url, headers)) {
            is Result.Success -> r.value
            is Result.Failure -> return r
        }
        return try {
            val gitlab = json.decodeFromString<List<GitLabReleaseDto>>(body)
            Result.success(gitlab.map { it.toGitHubShape() })
        } catch (error: SerializationException) {
            Result.failure(error.message ?: "gitlab_parse_failed", error)
        } catch (error: IllegalArgumentException) {
            Result.failure(error.message ?: "gitlab_parse_failed", error)
        }
    }

    private fun GitLabReleaseDto.toGitHubShape(): GitHubReleaseDto {
        val assets = assets?.links.orEmpty().mapNotNull { link ->
            val download = link.directAssetUrl?.takeIf { it.isNotBlank() }
                ?: link.url?.takeIf { it.isNotBlank() }
                ?: return@mapNotNull null
            GitHubAssetDto(name = link.name, browserDownloadUrl = download, size = 0)
        }
        return GitHubReleaseDto(
            tagName = tagName,
            name = name,
            body = ReleaseNotesText.normalize(description),
            prerelease = upcomingRelease || looksPre(tagName, name),
            assets = assets,
        )
    }

    private fun looksPre(tag: String, name: String?): Boolean {
        val blob = "$tag ${name.orEmpty()}".lowercase()
        return listOf("alpha", "beta", "rc", "pre", "nightly", "snapshot").any { blob.contains(it) }
    }
}

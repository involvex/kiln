package dev.involvex.kiln.feature.sources.codeberg

import android.os.Build
import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.core.model.GitHubReleaseChannel
import dev.involvex.kiln.core.model.ReleaseForge
import dev.involvex.kiln.core.network.KilnHttpClient
import dev.involvex.kiln.feature.sources.forge.ForgeTokenStore
import dev.involvex.kiln.feature.sources.github.GitHubAssetDto
import dev.involvex.kiln.feature.sources.github.GitHubReleaseDto
import dev.involvex.kiln.feature.sources.github.GitHubReleaseMatch
import dev.involvex.kiln.feature.sources.github.GitHubReleasePick
import dev.involvex.kiln.feature.sources.github.GitHubReleasePicker
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

@Serializable
private data class CodebergReleaseDto(
    @SerialName("tag_name") val tagName: String,
    val name: String? = null,
    val body: String? = null,
    @SerialName("prerelease") val prerelease: Boolean = false,
    val assets: List<CodebergAssetDto> = emptyList(),
)

@Serializable
private data class CodebergAssetDto(
    val name: String,
    @SerialName("browser_download_url") val browserDownloadUrl: String? = null,
    val size: Long = 0,
)

@Singleton
class CodebergReleasesClient @Inject constructor(private val http: KilnHttpClient, private val tokenStore: ForgeTokenStore) {
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
                Result.failure("no_matching_codeberg_asset")
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
        val url = "https://codeberg.org/api/v1/repos/$ownerRepo/releases?limit=$perPage"
        val headers = buildMap {
            put("Accept", "application/json")
            tokenStore.getToken(ReleaseForge.CODEBERG)?.let { put("Authorization", "token $it") }
        }
        val body = when (val r = http.getText(url, headers)) {
            is Result.Success -> r.value
            is Result.Failure -> return r
        }
        return try {
            val rows = json.decodeFromString<List<CodebergReleaseDto>>(body)
            Result.success(rows.map { it.toGitHubShape() })
        } catch (error: SerializationException) {
            Result.failure(error.message ?: "codeberg_parse_failed", error)
        } catch (error: IllegalArgumentException) {
            Result.failure(error.message ?: "codeberg_parse_failed", error)
        }
    }

    private fun CodebergReleaseDto.toGitHubShape(): GitHubReleaseDto = GitHubReleaseDto(
        tagName = tagName,
        name = name,
        body = body,
        prerelease = prerelease,
        assets = assets.mapNotNull { asset ->
            val url = asset.browserDownloadUrl?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            GitHubAssetDto(name = asset.name, browserDownloadUrl = url, size = asset.size)
        },
    )
}

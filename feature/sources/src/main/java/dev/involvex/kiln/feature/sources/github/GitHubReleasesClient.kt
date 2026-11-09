package dev.involvex.kiln.feature.sources.github

import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.core.model.GitHubReleaseChannel
import dev.involvex.kiln.core.network.KilnHttpClient
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

@Serializable
data class GitHubReleaseDto(
    @SerialName("tag_name") val tagName: String,
    val name: String? = null,
    @SerialName("prerelease") val prerelease: Boolean = false,
    val assets: List<GitHubAssetDto> = emptyList(),
)

@Serializable
data class GitHubAssetDto(val name: String, @SerialName("browser_download_url") val browserDownloadUrl: String, val size: Long = 0)

data class GitHubReleasePick(val tagName: String, val assetName: String, val downloadUrl: String, val sizeBytes: Long)

object GitHubReleasePicker {
    fun pick(releases: List<GitHubReleaseDto>, assetRegex: Regex, channel: GitHubReleaseChannel): GitHubReleasePick? {
        val includePre = channel != GitHubReleaseChannel.STABLE
        return releases
            .asSequence()
            .filter { includePre || !it.prerelease }
            .mapNotNull { release ->
                val asset = release.assets.firstOrNull { assetRegex.containsMatchIn(it.name) }
                    ?: return@mapNotNull null
                GitHubReleasePick(
                    tagName = release.tagName,
                    assetName = asset.name,
                    downloadUrl = asset.browserDownloadUrl,
                    sizeBytes = asset.size,
                )
            }
            .firstOrNull()
    }
}

@Singleton
class GitHubReleasesClient @Inject constructor(private val http: KilnHttpClient, private val tokenStore: GitHubTokenStore) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun latestMatchingRelease(ownerRepo: String, assetRegex: Regex, channel: GitHubReleaseChannel): Result<GitHubReleasePick> {
        val url = "https://api.github.com/repos/$ownerRepo/releases?per_page=20"
        val headers = buildMap {
            put("Accept", "application/vnd.github+json")
            tokenStore.getToken()?.let { put("Authorization", "Bearer $it") }
        }
        val body = when (val r = http.getText(url, headers)) {
            is Result.Success -> r.value
            is Result.Failure -> return r
        }
        return try {
            val releases = json.decodeFromString<List<GitHubReleaseDto>>(body)
            val pick = GitHubReleasePicker.pick(releases, assetRegex, channel)
            if (pick == null) {
                Result.failure("no_matching_github_asset")
            } else {
                Result.success(pick)
            }
        } catch (error: SerializationException) {
            Result.failure(error.message ?: "github_parse_failed", error)
        } catch (error: IllegalArgumentException) {
            Result.failure(error.message ?: "github_parse_failed", error)
        }
    }
}

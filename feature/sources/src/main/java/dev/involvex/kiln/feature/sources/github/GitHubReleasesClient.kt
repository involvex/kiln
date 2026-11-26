package dev.involvex.kiln.feature.sources.github

import android.os.Build
import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.core.model.GitHubReleaseChannel
import dev.involvex.kiln.core.network.KilnHttpClient
import dev.involvex.kiln.feature.sources.ReleaseNotesText
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
    val body: String? = null,
    @SerialName("prerelease") val prerelease: Boolean = false,
    val assets: List<GitHubAssetDto> = emptyList(),
)

@Serializable
data class GitHubAssetDto(val name: String, @SerialName("browser_download_url") val browserDownloadUrl: String, val size: Long = 0)

data class GitHubReleasePick(
    val tagName: String,
    val assetName: String,
    val downloadUrl: String,
    val sizeBytes: Long,
    val releaseNotes: String? = null,
)

/** [pick] plus the channel that produced it (STABLE preferred, optional PRE_RELEASE fallback). */
data class GitHubReleaseMatch(val pick: GitHubReleasePick, val channel: GitHubReleaseChannel)

object GitHubReleasePicker {
    fun pick(
        releases: List<GitHubReleaseDto>,
        assetRegex: Regex,
        channel: GitHubReleaseChannel,
        preferredAbis: List<String> = emptyList(),
    ): GitHubReleasePick? {
        val includePre = channel != GitHubReleaseChannel.STABLE
        return releases
            .asSequence()
            .filter { includePre || !it.prerelease }
            .mapNotNull { release ->
                val asset = preferAbiAsset(release.assets.filter { assetRegex.containsMatchIn(it.name) }, preferredAbis)
                    ?: return@mapNotNull null
                GitHubReleasePick(
                    tagName = release.tagName,
                    assetName = asset.name,
                    downloadUrl = asset.browserDownloadUrl,
                    sizeBytes = asset.size,
                    releaseNotes = ReleaseNotesText.normalize(release.body),
                )
            }
            .firstOrNull()
    }

    /**
     * Prefer [preferred] (usually STABLE). When [fallbackToPreRelease] is true and preferred is
     * STABLE with no APK match, retry as PRE_RELEASE so prerelease-only repos still resolve.
     * Does not redefine STABLE semantics for callers that pass fallback=false.
     */
    fun match(
        releases: List<GitHubReleaseDto>,
        assetRegex: Regex,
        preferred: GitHubReleaseChannel,
        preferredAbis: List<String> = emptyList(),
        fallbackToPreRelease: Boolean = false,
    ): GitHubReleaseMatch? {
        pick(releases, assetRegex, preferred, preferredAbis)?.let {
            return GitHubReleaseMatch(it, preferred)
        }
        if (fallbackToPreRelease &&
            preferred == GitHubReleaseChannel.STABLE
        ) {
            pick(releases, assetRegex, GitHubReleaseChannel.PRE_RELEASE, preferredAbis)?.let {
                return GitHubReleaseMatch(it, GitHubReleaseChannel.PRE_RELEASE)
            }
        }
        return null
    }

    /**
     * Among assets matching the regex, prefer a name that contains the device ABI
     * (e.g. arm64-v8a before armeabi-v7a). Falls back to the first match.
     */
    fun preferAbiAsset(assets: List<GitHubAssetDto>, preferredAbis: List<String>): GitHubAssetDto? {
        if (assets.isEmpty()) return null
        for (abi in preferredAbis) {
            if (abi.isBlank()) continue
            assets.firstOrNull { it.name.contains(abi, ignoreCase = true) }?.let { return it }
        }
        return assets.first()
    }
}

@Singleton
class GitHubReleasesClient @Inject constructor(private val http: KilnHttpClient, private val tokenStore: GitHubTokenStore) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun latestMatchingRelease(
        ownerRepo: String,
        assetRegex: Regex,
        channel: GitHubReleaseChannel,
        fallbackToPreRelease: Boolean = false,
    ): Result<GitHubReleasePick> = when (val matched = matchLatestRelease(ownerRepo, assetRegex, channel, fallbackToPreRelease)) {
        is Result.Failure -> matched
        is Result.Success -> Result.success(matched.value.pick)
    }

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
                Result.failure("no_matching_github_asset")
            } else {
                Result.success(match)
            }
        }
    }

    suspend fun listReleases(ownerRepo: String, perPage: Int = 20): Result<List<GitHubReleaseDto>> {
        val url = "https://api.github.com/repos/$ownerRepo/releases?per_page=$perPage"
        val headers = buildMap {
            put("Accept", "application/vnd.github+json")
            tokenStore.getToken()?.let { put("Authorization", "Bearer $it") }
        }
        val body = when (val r = http.getText(url, headers)) {
            is Result.Success -> r.value
            is Result.Failure -> return r
        }
        return try {
            Result.success(json.decodeFromString<List<GitHubReleaseDto>>(body))
        } catch (error: SerializationException) {
            Result.failure(error.message ?: "github_parse_failed", error)
        } catch (error: IllegalArgumentException) {
            Result.failure(error.message ?: "github_parse_failed", error)
        }
    }
}

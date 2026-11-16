package dev.involvex.kiln.feature.sources.github

import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.core.network.KilnHttpClient
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

@Serializable
data class GitHubStarredRepoDto(
    val id: Long,
    @SerialName("full_name") val fullName: String,
    val description: String? = null,
    val fork: Boolean = false,
    val private: Boolean = false,
    @SerialName("html_url") val htmlUrl: String? = null,
)

enum class StarredApkHint {
    UNKNOWN,
    HAS_APK,
    NO_APK,
    CHECK_FAILED,
}

data class GitHubStarredRepo(val ownerRepo: String, val description: String?, val fork: Boolean, val private: Boolean)

/**
 * Detects whether any recent release asset looks like an APK (name contains `.apk`, case-insensitive).
 * Stars are repos, not apps — this is only a heuristic for the review UI.
 */
object StarredApkHeuristic {
    private val APK_NAME = Regex("""(?i)\.apk(\.id)?$""")

    fun fromReleaseAssets(assetNames: List<String>): StarredApkHint {
        if (assetNames.isEmpty()) return StarredApkHint.NO_APK
        return if (assetNames.any { APK_NAME.containsMatchIn(it) }) {
            StarredApkHint.HAS_APK
        } else {
            StarredApkHint.NO_APK
        }
    }
}

@Singleton
class GitHubStarsClient @Inject constructor(private val http: KilnHttpClient, private val tokenStore: GitHubTokenStore) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun listStarred(maxPages: Int = DEFAULT_MAX_PAGES): Result<List<GitHubStarredRepo>> {
        val headers = authHeaders() ?: return Result.failure("github_pat_required")
        val collected = ArrayList<GitHubStarredRepo>()
        var page = 1
        var continuePaging = true
        while (continuePaging && page <= maxPages) {
            when (val pageResult = fetchStarredPage(page, headers)) {
                is Result.Failure -> return pageResult

                is Result.Success -> {
                    val pageRepos = pageResult.value
                    if (pageRepos.isEmpty()) {
                        continuePaging = false
                    } else {
                        collected += pageRepos
                        continuePaging = pageRepos.size >= PER_PAGE
                        page += 1
                    }
                }
            }
        }
        return Result.success(collected)
    }

    suspend fun apkHintFor(ownerRepo: String): Result<StarredApkHint> {
        val headers = authHeaders() ?: return Result.failure("github_pat_required")
        val url = "https://api.github.com/repos/$ownerRepo/releases?per_page=5"
        val body = when (val r = http.getText(url, headers)) {
            is Result.Success -> r.value
            is Result.Failure -> return r
        }
        return decodeApkHint(body)
    }

    private suspend fun fetchStarredPage(page: Int, headers: Map<String, String>): Result<List<GitHubStarredRepo>> {
        val url = "https://api.github.com/user/starred?per_page=$PER_PAGE&page=$page&sort=updated"
        val body = when (val r = http.getText(url, headers)) {
            is Result.Success -> r.value
            is Result.Failure -> return r
        }
        return decodeStarredPage(body)
    }

    private fun decodeStarredPage(body: String): Result<List<GitHubStarredRepo>> = try {
        val pageRepos = json.decodeFromString<List<GitHubStarredRepoDto>>(body)
        Result.success(
            pageRepos.map {
                GitHubStarredRepo(
                    ownerRepo = it.fullName,
                    description = it.description,
                    fork = it.fork,
                    private = it.private,
                )
            },
        )
    } catch (error: SerializationException) {
        Result.failure(error.message ?: "github_stars_parse_failed", error)
    } catch (error: IllegalArgumentException) {
        Result.failure(error.message ?: "github_stars_parse_failed", error)
    }

    private fun decodeApkHint(body: String): Result<StarredApkHint> = try {
        val releases = json.decodeFromString<List<GitHubReleaseDto>>(body)
        val names = releases.flatMap { release -> release.assets.map { it.name } }
        Result.success(StarredApkHeuristic.fromReleaseAssets(names))
    } catch (error: SerializationException) {
        Result.failure(error.message ?: "github_stars_parse_failed", error)
    } catch (error: IllegalArgumentException) {
        Result.failure(error.message ?: "github_stars_parse_failed", error)
    }

    private fun authHeaders(): Map<String, String>? {
        val token = tokenStore.getToken() ?: return null
        return mapOf(
            "Authorization" to "Bearer $token",
            "Accept" to "application/vnd.github+json",
            "X-GitHub-Api-Version" to "2022-11-28",
        )
    }

    private companion object {
        const val PER_PAGE = 100
        const val DEFAULT_MAX_PAGES = 3
    }
}

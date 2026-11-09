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
data class GitHubWorkflowRunsResponse(@SerialName("workflow_runs") val workflowRuns: List<GitHubWorkflowRunDto> = emptyList())

@Serializable
data class GitHubWorkflowRunDto(
    val id: Long,
    @SerialName("run_number") val runNumber: Long = 0,
    @SerialName("head_sha") val headSha: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    val status: String? = null,
    val conclusion: String? = null,
)

@Serializable
data class GitHubArtifactsResponse(val artifacts: List<GitHubArtifactDto> = emptyList())

@Serializable
data class GitHubArtifactDto(
    val id: Long,
    val name: String,
    @SerialName("size_in_bytes") val sizeInBytes: Long = 0,
    @SerialName("archive_download_url") val archiveDownloadUrl: String,
    val expired: Boolean = false,
)

data class GitHubArtifactPick(
    val artifactId: Long,
    val artifactName: String,
    val downloadUrl: String,
    val sizeBytes: Long,
    val runNumber: Long,
    val versionLabel: String,
)

/**
 * Picks the newest successful Actions run that has a non-expired artifact matching [artifactNameRegex]
 * (falls back to the first non-expired artifact when none match the regex).
 */
object GitHubArtifactPicker {
    val DEFAULT_ARTIFACT_NAME_REGEX: Regex = Regex("(?i).*(apk|nightly|release).*")

    fun pick(
        runs: List<GitHubWorkflowRunDto>,
        artifactsByRunId: Map<Long, List<GitHubArtifactDto>>,
        artifactNameRegex: Regex = DEFAULT_ARTIFACT_NAME_REGEX,
    ): GitHubArtifactPick? {
        for (run in runs.filter { it.conclusion.equals("success", ignoreCase = true) }) {
            val artifacts = artifactsByRunId[run.id].orEmpty().filter { !it.expired }
            if (artifacts.isEmpty()) continue
            val matched = artifacts.firstOrNull { artifactNameRegex.containsMatchIn(it.name) }
                ?: artifacts.first()
            val label = "nightly-${run.runNumber}".takeIf { run.runNumber > 0 }
                ?: "nightly-${run.id}"
            return GitHubArtifactPick(
                artifactId = matched.id,
                artifactName = matched.name,
                downloadUrl = matched.archiveDownloadUrl,
                sizeBytes = matched.sizeInBytes,
                runNumber = run.runNumber,
                versionLabel = label,
            )
        }
        return null
    }
}

@Singleton
class GitHubActionsClient @Inject constructor(private val http: KilnHttpClient, private val tokenStore: GitHubTokenStore) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun latestMatchingArtifact(
        ownerRepo: String,
        artifactNameRegex: Regex = GitHubArtifactPicker.DEFAULT_ARTIFACT_NAME_REGEX,
    ): Result<GitHubArtifactPick> {
        val headers = authHeaders() ?: return Result.failure("github_pat_required")
        val runs = when (val loaded = loadSuccessfulRuns(ownerRepo, headers)) {
            is Result.Failure -> return loaded
            is Result.Success -> loaded.value
        }
        val artifactsByRun = loadArtifactsForRuns(ownerRepo, runs, headers)
        val pick = GitHubArtifactPicker.pick(runs, artifactsByRun, artifactNameRegex)
            ?: return Result.failure("no_matching_github_artifact")
        return Result.success(pick)
    }

    private fun authHeaders(): Map<String, String>? {
        val token = tokenStore.getToken() ?: return null
        return mapOf(
            "Authorization" to "Bearer $token",
            "Accept" to "application/vnd.github+json",
            "X-GitHub-Api-Version" to "2022-11-28",
        )
    }

    private suspend fun loadSuccessfulRuns(ownerRepo: String, headers: Map<String, String>): Result<List<GitHubWorkflowRunDto>> {
        val runsUrl =
            "https://api.github.com/repos/$ownerRepo/actions/runs?status=completed&per_page=15"
        val runsBody = when (val r = http.getText(runsUrl, headers)) {
            is Result.Success -> r.value
            is Result.Failure -> return r
        }
        return try {
            Result.success(json.decodeFromString<GitHubWorkflowRunsResponse>(runsBody).workflowRuns)
        } catch (error: SerializationException) {
            Result.failure(error.message ?: "github_actions_parse_failed", error)
        } catch (error: IllegalArgumentException) {
            Result.failure(error.message ?: "github_actions_parse_failed", error)
        }
    }

    private suspend fun loadArtifactsForRuns(
        ownerRepo: String,
        runs: List<GitHubWorkflowRunDto>,
        headers: Map<String, String>,
    ): Map<Long, List<GitHubArtifactDto>> {
        val artifactsByRun = linkedMapOf<Long, List<GitHubArtifactDto>>()
        for (run in runs.filter { it.conclusion.equals("success", ignoreCase = true) }) {
            val list = fetchArtifacts(ownerRepo, run.id, headers)
            if (list.isNotEmpty()) {
                artifactsByRun[run.id] = list
            }
        }
        return artifactsByRun
    }

    private suspend fun fetchArtifacts(ownerRepo: String, runId: Long, headers: Map<String, String>): List<GitHubArtifactDto> {
        val artifactsUrl =
            "https://api.github.com/repos/$ownerRepo/actions/runs/$runId/artifacts"
        return when (val artResult = http.getText(artifactsUrl, headers)) {
            is Result.Failure -> emptyList()

            is Result.Success -> try {
                json.decodeFromString<GitHubArtifactsResponse>(artResult.value).artifacts
            } catch (_: SerializationException) {
                emptyList()
            } catch (_: IllegalArgumentException) {
                emptyList()
            }
        }
    }
}

package dev.involvex.kiln.core.model

import dev.involvex.kiln.core.common.Result

enum class MatchConfidence {
    HIGH,
    MEDIUM,
    WARNING,
    NONE,
}

enum class SourceKind {
    FDROID,
    GITHUB,
}

/** Update channel for a GitHub-managed app (Releases vs Actions artifacts). */
enum class GitHubReleaseChannel {
    STABLE,
    PRE_RELEASE,
    NIGHTLY,
}

fun parseGitHubReleaseChannel(raw: String?): GitHubReleaseChannel = runCatching { GitHubReleaseChannel.valueOf(raw?.trim().orEmpty()) }
    .getOrDefault(GitHubReleaseChannel.STABLE)

data class RepoConfig(
    val id: String,
    val name: String,
    val address: String,
    val fingerprint: String,
    val kind: SourceKind = SourceKind.FDROID,
    val enabled: Boolean = true,
    val lastSyncedAtEpochMs: Long? = null,
    /** Comma-separated alternate HTTPS repo bases (failover after [address]). */
    val mirrorsCsv: String = "",
)

data class IndexedPackage(
    val packageName: String,
    val name: String,
    val summary: String?,
    val sourceCodeUrl: String?,
    val repoId: String,
    val categories: List<String> = emptyList(),
    val signerSha256: List<String>,
    val suggestedVersionCode: Long?,
    val suggestedVersionName: String?,
)

data class IndexedRelease(
    val packageName: String,
    val repoId: String,
    val versionName: String,
    val versionCode: Long,
    val apkName: String,
    val apkSha256: String?,
    val sizeBytes: Long?,
    val signerSha256: List<String>,
    val whatsNew: String? = null,
)

data class ManagedApp(
    val packageName: String,
    val sourceId: String,
    val confidence: MatchConfidence,
    val pinned: Boolean = false,
    val ignoreUpdates: Boolean = false,
    val githubOwnerRepo: String? = null,
    val githubAssetRegex: String? = null,
    val githubChannel: GitHubReleaseChannel = GitHubReleaseChannel.STABLE,
    /** True when the user watches an uninstalled catalog/GitHub app for new versions. */
    val watchOnly: Boolean = false,
)

data class AppUpdateInfo(
    val packageName: String,
    val availableVersionName: String,
    val availableVersionCode: Long,
    val sourceId: String,
    val sourceLabel: String,
    val confidence: MatchConfidence,
    val signatureMatches: Boolean,
    /** HTTPS URL for the single APK or Actions artifact zip (null = cannot install yet). */
    val downloadUrl: String? = null,
    val apkSha256: String? = null,
    val sizeBytes: Long? = null,
    /** When true, [downloadUrl] needs a GitHub PAT (Actions artifact zip). */
    val requiresGitHubAuth: Boolean = false,
    /** Regex used to pick an APK inside an Actions artifact zip. */
    val apkAssetRegex: String? = null,
    /** Plain-text release notes / whatsNew (truncated). */
    val releaseNotes: String? = null,
    /** True when this update is for a watch-only (not installed) managed row. */
    val watchOnly: Boolean = false,
)

/** Watched uninstalled package shown on Sources. */
data class WatchlistEntry(
    val packageName: String,
    val displayName: String,
    val sourceId: String,
    val githubOwnerRepo: String? = null,
    val availableVersionName: String? = null,
    val hasUpdate: Boolean = false,
    val releaseNotes: String? = null,
)

/**
 * Read-only installed-app snapshot for matching / update checks.
 * Implemented in `:feature:apps`, bound in `:app`.
 */
interface InstalledAppsReader {
    suspend fun userApps(): List<InstalledAppMatchInput>
}

data class InstalledAppMatchInput(
    val packageName: String,
    val label: String,
    val versionName: String?,
    val versionCode: Long,
    val signerSha256: List<String>,
)

data class UpdateCheckResult(val reposSynced: Int, val reposFailed: Int, val updatesFound: Int)

/** Persisted update-source binding for an installed package. */
data class ManagedSourceLink(
    val sourceId: String,
    val confidence: MatchConfidence,
    val pinned: Boolean,
    val githubOwnerRepo: String? = null,
    val githubChannel: GitHubReleaseChannel = GitHubReleaseChannel.STABLE,
)

/**
 * Available updates for managed / matched packages.
 * Implemented in `:feature:sources`, consumed by `:feature:apps`.
 */
interface AppUpdateRepository {
    fun updates(): kotlinx.coroutines.flow.Flow<Map<String, AppUpdateInfo>>

    /** Recompute update map from the local index + GitHub (no repo download). */
    suspend fun refreshUpdates()

    /**
     * Sync enabled F-Droid-format repos, then [refreshUpdates].
     * Use from the Apps “Check for updates” action.
     */
    suspend fun checkForUpdates(): Result<UpdateCheckResult>

    /** `null` if the package is not a managed app yet. */
    fun observeIgnoreUpdates(packageName: String): kotlinx.coroutines.flow.Flow<Boolean?>

    /** `null` if the package is not a managed app yet. */
    fun observeManagedLink(packageName: String): kotlinx.coroutines.flow.Flow<ManagedSourceLink?>

    /** Enabled F-Droid-format repos for manual source linking. */
    fun observeEnabledRepos(): kotlinx.coroutines.flow.Flow<List<RepoConfig>>

    /**
     * Persist [ignoreUpdates] for [packageName]. Creates a minimal managed row when missing
     * (source from current update info or `"manual"`).
     */
    suspend fun setIgnoreUpdates(packageName: String, ignoreUpdates: Boolean): Result<Unit>

    /**
     * Manually bind an installed package to an update source (F-Droid repo id or `github:owner/repo`).
     * Sets [pinned] so later auto-match / priority does not silently override the choice.
     */
    suspend fun linkManagedSource(
        packageName: String,
        sourceId: String,
        confidence: MatchConfidence,
        githubOwnerRepo: String? = null,
        githubAssetRegex: String? = null,
        pinned: Boolean = true,
        githubChannel: GitHubReleaseChannel? = null,
    ): Result<Unit>

    /**
     * Sets the GitHub update channel for a managed app. No-op failure when the package
     * is not linked to a GitHub owner/repo.
     */
    suspend fun setGitHubChannel(packageName: String, channel: GitHubReleaseChannel): Result<Unit>
}

/** Watch uninstalled catalog/GitHub apps for new versions without installing. */
interface WatchlistRepository {
    /** Watch an uninstalled F-Droid catalog package for newer index versions. */
    suspend fun watchCatalogPackage(packageName: String, repoId: String): Result<Unit>

    /** Remove watch-only status (deletes the managed row when it was watch-only). */
    suspend fun unwatchPackage(packageName: String): Result<Unit>

    fun observeWatchlist(): kotlinx.coroutines.flow.Flow<List<WatchlistEntry>>

    /** Whether [packageName] is currently watch-only. */
    fun observeIsWatching(packageName: String): kotlinx.coroutines.flow.Flow<Boolean>
}

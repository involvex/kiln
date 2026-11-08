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

data class RepoConfig(
    val id: String,
    val name: String,
    val address: String,
    val fingerprint: String,
    val kind: SourceKind = SourceKind.FDROID,
    val enabled: Boolean = true,
    val lastSyncedAtEpochMs: Long? = null,
)

data class IndexedPackage(
    val packageName: String,
    val name: String,
    val summary: String?,
    val sourceCodeUrl: String?,
    val repoId: String,
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
)

data class ManagedApp(
    val packageName: String,
    val sourceId: String,
    val confidence: MatchConfidence,
    val pinned: Boolean = false,
    val ignoreUpdates: Boolean = false,
    val githubOwnerRepo: String? = null,
    val githubAssetRegex: String? = null,
    val includePreReleases: Boolean = false,
)

data class AppUpdateInfo(
    val packageName: String,
    val availableVersionName: String,
    val availableVersionCode: Long,
    val sourceId: String,
    val sourceLabel: String,
    val confidence: MatchConfidence,
    val signatureMatches: Boolean,
    /** HTTPS URL for the single APK (null = cannot install yet). */
    val downloadUrl: String? = null,
    val apkSha256: String? = null,
    val sizeBytes: Long? = null,
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

    /**
     * Persist [ignoreUpdates] for [packageName]. Creates a minimal managed row when missing
     * (source from current update info or `"manual"`).
     */
    suspend fun setIgnoreUpdates(packageName: String, ignoreUpdates: Boolean): Result<Unit>
}

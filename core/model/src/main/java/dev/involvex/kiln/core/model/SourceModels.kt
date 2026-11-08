package dev.involvex.kiln.core.model

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

/**
 * Available updates for managed / matched packages.
 * Implemented in `:feature:sources`, consumed by `:feature:apps`.
 */
interface AppUpdateRepository {
    fun updates(): kotlinx.coroutines.flow.Flow<Map<String, AppUpdateInfo>>

    suspend fun refreshUpdates()
}

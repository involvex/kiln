package dev.involvex.kiln.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "repos")
data class RepoEntity(
    @PrimaryKey val id: String,
    val name: String,
    val address: String,
    val fingerprint: String,
    val kind: String,
    val enabled: Boolean,
    val lastSyncedAtEpochMs: Long?,
    val priority: Int,
)

@Entity(
    tableName = "indexed_packages",
    indices = [Index(value = ["packageName"]), Index(value = ["repoId"])],
)
data class IndexedPackageEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val packageName: String,
    val repoId: String,
    val name: String,
    val summary: String?,
    val sourceCodeUrl: String?,
    /** Comma-separated F-Droid categories; empty when unknown. */
    val categoriesCsv: String = "",
    /** Comma-separated F-Droid anti-feature ids; empty when none. */
    val antiFeaturesCsv: String = "",
    val signerSha256Csv: String,
    val suggestedVersionCode: Long?,
    val suggestedVersionName: String?,
    /** Relative path to icon within the repo (index-v2 `metadata.icon`). */
    val iconPath: String? = null,
    /** Comma-separated relative screenshot paths (`metadata.phoneScreenshots`). */
    val phoneScreenshotsCsv: String = "",
)

@Entity(
    tableName = "indexed_releases",
    indices = [Index(value = ["packageName", "repoId"])],
)
data class IndexedReleaseEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val packageName: String,
    val repoId: String,
    val versionName: String,
    val versionCode: Long,
    val apkName: String,
    val apkSha256: String?,
    val sizeBytes: Long?,
    val signerSha256Csv: String,
    /** F-Droid index-v2 `whatsNew` (localized plain text); null when absent. */
    val whatsNew: String? = null,
)

@Entity(tableName = "managed_apps")
data class ManagedAppEntity(
    @PrimaryKey val packageName: String,
    val sourceId: String,
    val confidence: String,
    val pinned: Boolean,
    val ignoreUpdates: Boolean,
    val githubOwnerRepo: String?,
    val githubAssetRegex: String?,
    /** [dev.involvex.kiln.core.model.GitHubReleaseChannel] name; default STABLE. */
    val githubChannel: String = "STABLE",
    /** True when watching an uninstalled app for new versions (no local install required). */
    val watchOnly: Boolean = false,
)

@Entity(
    tableName = "install_jobs",
    indices = [Index(value = ["packageName"]), Index(value = ["status"])],
)
data class InstallJobEntity(
    @PrimaryKey val jobId: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val downloadUrl: String,
    val expectedSha256: String?,
    val sizeBytes: Long?,
    val status: String,
    val progressPercent: Int = 0,
    val errorMessage: String? = null,
    val localApkPath: String? = null,
    val requiresGitHubAuth: Boolean = false,
    val apkAssetRegex: String? = null,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
)

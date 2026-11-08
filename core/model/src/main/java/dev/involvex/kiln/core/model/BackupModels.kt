package dev.involvex.kiln.core.model

/** What a backup archive actually contains (honest tier messaging). */
enum class BackupContentTier {
    NONE,
    SHIZUKU,
    ROOT,
}

data class BackupAppCandidate(
    val packageName: String,
    val label: String,
    val versionName: String?,
    val versionCode: Long,
    val isSystem: Boolean,
    val hasSplits: Boolean,
    val apkSizeBytes: Long,
)

data class BackupArchiveMetadata(
    val formatVersion: Int = 1,
    val packageName: String,
    val label: String,
    val versionName: String?,
    val versionCode: Long,
    val signerSha256: List<String>,
    val tier: BackupContentTier,
    val createdAtEpochMs: Long,
    val includesAppData: Boolean,
    val includesAndroidData: Boolean,
    val apkEntries: List<String>,
    val hasSplits: Boolean,
)

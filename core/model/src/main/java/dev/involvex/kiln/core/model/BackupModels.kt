package dev.involvex.kiln.core.model

import kotlinx.coroutines.flow.Flow

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
    /** True when the on-disk container is AES-GCM wrapped (outer file). */
    val encrypted: Boolean = false,
    val androidDataEntry: String? = null,
    val androidObbEntry: String? = null,
)

data class BackupPrefs(val treeUri: String? = null, val keepN: Int = DEFAULT_KEEP_N) {
    companion object {
        const val DEFAULT_KEEP_N = 3
        const val MIN_KEEP_N = 1
        const val MAX_KEEP_N = 10

        fun clampKeepN(value: Int): Int = value.coerceIn(MIN_KEEP_N, MAX_KEEP_N)
    }
}

/** Persisted backup destination + rotation (DataStore in `:feature:backup`). */
interface BackupPreferences {
    fun observe(): Flow<BackupPrefs>

    suspend fun current(): BackupPrefs

    suspend fun setTreeUri(uri: String?)

    suspend fun setKeepN(keepN: Int)
}

/** Named archive under a package folder in the SAF tree (for rotation). */
data class BackupTreeEntry(val displayName: String, val uriString: String, val lastModifiedEpochMs: Long)

object BackupRotation {
    /**
     * Returns entries to delete so that at most [keepN] newest remain
     * (by [BackupTreeEntry.lastModifiedEpochMs] descending).
     */
    fun selectForDeletion(entries: List<BackupTreeEntry>, keepN: Int): List<BackupTreeEntry> {
        val n = BackupPrefs.clampKeepN(keepN)
        if (entries.size <= n) return emptyList()
        return entries.sortedByDescending { it.lastModifiedEpochMs }.drop(n)
    }
}

package dev.involvex.kiln.core.model

/** Per-app breakdown from [android.app.usage.StorageStatsManager]. */
data class AppStorageStats(
    val packageName: String,
    val label: String,
    val isSystem: Boolean,
    val appBytes: Long,
    val dataBytes: Long,
    val cacheBytes: Long,
) {
    val totalBytes: Long get() = appBytes + dataBytes + cacheBytes
}

data class StorageOverview(val apps: List<AppStorageStats>, val totalAppBytes: Long, val totalDataBytes: Long, val totalCacheBytes: Long) {
    val totalBytes: Long get() = totalAppBytes + totalDataBytes + totalCacheBytes
}

/** Fixed shared-storage roots for orphan Android/data + obb cleanup (shell allowlist). */
enum class AndroidSharedRoot(val pathKey: String, val absolutePath: String) {
    DATA("data", "/storage/emulated/0/Android/data"),
    OBB("obb", "/storage/emulated/0/Android/obb"),
    ;

    companion object {
        fun fromPathKey(key: String): AndroidSharedRoot? = entries.firstOrNull { it.pathKey == key }
    }
}

/** Leftover APK-like file found under Downloads / SAF tree. */
data class ApkLeftover(
    val id: String,
    val displayName: String,
    val sizeBytes: Long,
    val uriString: String,
    val relativePath: String = displayName,
)

/** Any file under a user-picked SAF tree (used for large-file + duplicate scans). */
data class SafTreeFile(val id: String, val displayName: String, val sizeBytes: Long, val uriString: String, val relativePath: String)

/** Same-size, same-hash files under one SAF tree. */
data class DuplicateFileGroup(val sizeBytes: Long, val sha256: String, val files: List<SafTreeFile>)

/** Soft-deleted file sitting in `.kiln-trash/` with undo metadata. */
data class KilnTrashEntry(
    val id: String,
    val originalRelativePath: String,
    val trashFileName: String,
    val displayName: String,
    val sizeBytes: Long,
    val movedAtEpochMs: Long,
    val uriString: String,
)

/** Package-named directory under Android/data or Android/obb with no matching install. */
data class OrphanStorageDir(val root: AndroidSharedRoot, val packageName: String, val sizeBytes: Long)

/**
 * Last SAF folder used for leftovers / large files / trash.
 * Implemented in `:feature:storage`, bound in `:app`.
 */
interface StorageTreePreferences {
    suspend fun treeUri(): String?

    suspend fun setTreeUri(uri: String?)
}

/**
 * Age-prunes `.kiln-trash` under the persisted SAF tree.
 * Implemented in `:feature:storage`, used by scheduled maintenance.
 */
interface KilnTrashJanitor {
    /** Delete trash entries older than [maxAgeMs]. Returns bytes freed. */
    suspend fun pruneOlderThan(maxAgeMs: Long, nowEpochMs: Long): Long
}

/** Pure helpers for leftover matching and orphan filtering (unit-tested). */
object StoragePhase2Filter {
    private val LEFTOVER_EXT = Regex(".*\\.(apk|apks|xapk)$", RegexOption.IGNORE_CASE)

    fun isApkLeftoverName(fileName: String): Boolean = LEFTOVER_EXT.matches(fileName.trim())

    fun orphanPackageDirs(dirNames: Collection<String>, installedPackages: Set<String>): List<String> = dirNames
        .asSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() && it !in installedPackages }
        .filter { looksLikePackageName(it) }
        .distinct()
        .sorted()
        .toList()

    fun topAppsBySize(apps: List<AppStorageStats>, limit: Int = DEFAULT_SIZE_BAR_LIMIT): List<AppStorageStats> =
        apps.sortedByDescending { it.totalBytes }.take(limit.coerceAtLeast(0))

    fun looksLikePackageName(value: String): Boolean {
        if (value.length !in PACKAGE_NAME_MIN_LEN..PACKAGE_NAME_MAX_LEN) return false
        if (value.contains("..") || value.contains('/') || value.contains('\\')) return false
        return PACKAGE_NAME.matches(value)
    }

    private val PACKAGE_NAME = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$")

    const val DEFAULT_SIZE_BAR_LIMIT = 8
    private const val PACKAGE_NAME_MIN_LEN = 3
    private const val PACKAGE_NAME_MAX_LEN = 255
}

/**
 * Pure helpers for large-file thresholding and same-size duplicate grouping (unit-tested).
 * Hashing stays in the repository; this only groups candidates and builds groups from digests.
 */
object StorageCleanerLogic {
    const val LARGE_FILE_THRESHOLD_BYTES = 50L * 1024L * 1024L
    const val LARGE_FILE_LIMIT = 100
    const val TRASH_DIR_NAME = ".kiln-trash"
    const val TRASH_MANIFEST_NAME = "manifest.tsv"
    const val TRASH_MAX_AGE_DAYS = 14L
    const val MAX_HASH_CANDIDATES = 200

    fun largeFiles(
        files: List<SafTreeFile>,
        thresholdBytes: Long = LARGE_FILE_THRESHOLD_BYTES,
        limit: Int = LARGE_FILE_LIMIT,
    ): List<SafTreeFile> = files
        .asSequence()
        .filter { it.sizeBytes >= thresholdBytes }
        .filter { !StoragePhase2Filter.isApkLeftoverName(it.displayName) }
        .sortedByDescending { it.sizeBytes }
        .take(limit.coerceAtLeast(0))
        .toList()

    /** Groups files that share an identical positive size (duplicate hash candidates). */
    fun sameSizeGroups(files: List<SafTreeFile>, minGroupSize: Int = 2): Map<Long, List<SafTreeFile>> = files
        .asSequence()
        .filter { it.sizeBytes > 0L }
        .groupBy { it.sizeBytes }
        .filterValues { it.size >= minGroupSize }

    /**
     * Builds duplicate groups from (file, sha256) pairs that already share the same size.
     * Only groups with at least two matching digests are returned.
     */
    fun duplicateGroupsFromHashes(hashed: List<Pair<SafTreeFile, String>>, minGroupSize: Int = 2): List<DuplicateFileGroup> = hashed
        .groupBy { (file, digest) -> file.sizeBytes to digest.lowercase() }
        .values
        .asSequence()
        .filter { it.size >= minGroupSize }
        .map { group ->
            val (file, digest) = group.first()
            DuplicateFileGroup(
                sizeBytes = file.sizeBytes,
                sha256 = digest.lowercase(),
                files = group.map { it.first }.sortedBy { it.relativePath.lowercase() },
            )
        }
        .sortedByDescending { it.sizeBytes }
        .toList()

    /** Flatten same-size groups into a capped candidate list for hashing (largest sizes first). */
    fun hashCandidates(sameSize: Map<Long, List<SafTreeFile>>, maxFiles: Int = MAX_HASH_CANDIDATES): List<SafTreeFile> = sameSize.entries
        .sortedByDescending { it.key }
        .flatMap { it.value }
        .take(maxFiles.coerceAtLeast(0))
}

/** Gate for orphan mutate actions. */
object StorageOrphanGate {
    fun canAccessAndroidData(capabilities: Set<PrivilegeCapability>): Boolean = PrivilegeCapability.ACCESS_ANDROID_DATA in capabilities
}

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
data class ApkLeftover(val id: String, val displayName: String, val sizeBytes: Long, val uriString: String)

/** Package-named directory under Android/data or Android/obb with no matching install. */
data class OrphanStorageDir(val root: AndroidSharedRoot, val packageName: String, val sizeBytes: Long)

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

/** Gate for orphan mutate actions. */
object StorageOrphanGate {
    fun canAccessAndroidData(capabilities: Set<PrivilegeCapability>): Boolean = PrivilegeCapability.ACCESS_ANDROID_DATA in capabilities
}

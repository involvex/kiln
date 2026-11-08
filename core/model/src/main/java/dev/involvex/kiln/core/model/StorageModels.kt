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

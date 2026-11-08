package dev.involvex.kiln.feature.storage

import dev.involvex.kiln.core.model.AppStorageStats

enum class StoragePermissionState {
    UNKNOWN,
    MISSING,
    GRANTED,
}

data class PendingClearAction(val packageName: String?, val label: String, val cacheBytes: Long)

data class StorageUiState(
    val permission: StoragePermissionState = StoragePermissionState.UNKNOWN,
    val isLoading: Boolean = false,
    val includeSystem: Boolean = false,
    val apps: List<AppStorageStats> = emptyList(),
    val totalAppBytes: Long = 0,
    val totalDataBytes: Long = 0,
    val totalCacheBytes: Long = 0,
    val query: String = "",
    val canClearPrivileged: Boolean = false,
    val pendingClear: PendingClearAction? = null,
    val clearing: Boolean = false,
    val message: String? = null,
    val error: String? = null,
) {
    val filteredApps: List<AppStorageStats>
        get() {
            val q = query.trim()
            if (q.isEmpty()) return apps
            return apps.filter {
                it.label.contains(q, ignoreCase = true) ||
                    it.packageName.contains(q, ignoreCase = true)
            }
        }

    val totalBytes: Long get() = totalAppBytes + totalDataBytes + totalCacheBytes
}

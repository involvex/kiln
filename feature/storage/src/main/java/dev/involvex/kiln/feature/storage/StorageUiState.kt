package dev.involvex.kiln.feature.storage

import dev.involvex.kiln.core.model.ApkLeftover
import dev.involvex.kiln.core.model.AppStorageStats
import dev.involvex.kiln.core.model.OrphanStorageDir
import dev.involvex.kiln.core.model.StoragePhase2Filter

enum class StoragePermissionState {
    UNKNOWN,
    MISSING,
    GRANTED,
}

data class PendingClearAction(val packageName: String?, val label: String, val cacheBytes: Long)

sealed interface PendingStorageDelete {
    data class Leftovers(val items: List<ApkLeftover>) : PendingStorageDelete

    data class Orphans(val items: List<OrphanStorageDir>) : PendingStorageDelete
}

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
    val canAccessAndroidData: Boolean = false,
    val pendingClear: PendingClearAction? = null,
    val clearing: Boolean = false,
    val leftovers: List<ApkLeftover> = emptyList(),
    val selectedLeftoverIds: Set<String> = emptySet(),
    val leftoversLoading: Boolean = false,
    val orphans: List<OrphanStorageDir> = emptyList(),
    val selectedOrphans: Set<String> = emptySet(),
    val orphansLoading: Boolean = false,
    val orphansError: String? = null,
    val pendingDelete: PendingStorageDelete? = null,
    val deleting: Boolean = false,
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

    val sizeBarApps: List<AppStorageStats>
        get() = StoragePhase2Filter.topAppsBySize(filteredApps)

    fun orphanKey(orphan: OrphanStorageDir): String = "${orphan.root.pathKey}:${orphan.packageName}"
}

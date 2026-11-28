package dev.involvex.kiln.feature.storage

import dev.involvex.kiln.core.model.ApkLeftover
import dev.involvex.kiln.core.model.AppStorageStats
import dev.involvex.kiln.core.model.DuplicateFileGroup
import dev.involvex.kiln.core.model.KilnTrashEntry
import dev.involvex.kiln.core.model.OrphanStorageDir
import dev.involvex.kiln.core.model.SafTreeFile
import dev.involvex.kiln.core.model.StorageTreemapLeaf
import dev.involvex.kiln.core.model.StorageTreemapLeaves

enum class StoragePermissionState {
    UNKNOWN,
    MISSING,
    GRANTED,
}

data class PendingClearAction(val packageName: String?, val label: String, val cacheBytes: Long)

sealed interface PendingStorageDelete {
    data class Leftovers(val items: List<ApkLeftover>) : PendingStorageDelete

    data class LargeFiles(val items: List<SafTreeFile>) : PendingStorageDelete

    data class Duplicates(val items: List<SafTreeFile>) : PendingStorageDelete

    data class TrashRestore(val items: List<KilnTrashEntry>) : PendingStorageDelete

    data class TrashPurge(val items: List<KilnTrashEntry>) : PendingStorageDelete

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
    val treeUri: String? = null,
    val leftovers: List<ApkLeftover> = emptyList(),
    val selectedLeftoverIds: Set<String> = emptySet(),
    val largeFiles: List<SafTreeFile> = emptyList(),
    val selectedLargeFileIds: Set<String> = emptySet(),
    val duplicates: List<DuplicateFileGroup> = emptyList(),
    val selectedDuplicateIds: Set<String> = emptySet(),
    val trash: List<KilnTrashEntry> = emptyList(),
    val selectedTrashIds: Set<String> = emptySet(),
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

    val leftoversBytes: Long get() = leftovers.sumOf { it.sizeBytes }

    val orphansBytes: Long get() = orphans.sumOf { it.sizeBytes }

    val largeFilesBytes: Long get() = largeFiles.sumOf { it.sizeBytes }

    fun treemapLeaves(leftoversLabel: String, orphansLabel: String, largeFilesLabel: String): List<StorageTreemapLeaf> =
        StorageTreemapLeaves.from(
            apps = filteredApps,
            leftoversBytes = leftoversBytes,
            orphansBytes = orphansBytes,
            largeFilesBytes = largeFilesBytes,
            leftoversLabel = leftoversLabel,
            orphansLabel = orphansLabel,
            largeFilesLabel = largeFilesLabel,
        )

    fun orphanKey(orphan: OrphanStorageDir): String = "${orphan.root.pathKey}:${orphan.packageName}"
}

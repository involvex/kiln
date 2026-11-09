package dev.involvex.kiln.core.model

/**
 * Pure selection of rollback entries to delete: age first, then oldest until under size cap.
 */
object RollbackPrunePolicy {
    fun selectForDeletion(entries: List<RollbackApkEntry>, maxAgeMs: Long, maxTotalBytes: Long, nowEpochMs: Long): List<RollbackApkEntry> {
        if (entries.isEmpty()) return emptyList()
        val cutoff = nowEpochMs - maxAgeMs
        val byAge = entries.filter { it.savedAtEpochMs < cutoff }.toMutableSet()
        val remaining = entries.filterNot { it in byAge }.sortedBy { it.savedAtEpochMs }
        var total = remaining.sumOf { it.sizeBytes }
        val bySize = mutableListOf<RollbackApkEntry>()
        for (entry in remaining) {
            if (total <= maxTotalBytes) break
            bySize.add(entry)
            total -= entry.sizeBytes
        }
        return (byAge + bySize).distinct()
    }
}

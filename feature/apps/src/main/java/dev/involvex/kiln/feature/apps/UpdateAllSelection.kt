package dev.involvex.kiln.feature.apps

import dev.involvex.kiln.core.model.AppUpdateInfo

/**
 * Which updates may be batch-enqueued from Apps (matches App detail Install rules).
 */
object UpdateAllSelection {
    data class Partition(val installable: List<AppUpdateInfo>, val skippedCount: Int)

    fun partition(updates: Collection<AppUpdateInfo>): Partition {
        val candidates = updates.filter { !it.watchOnly }
        val installable = candidates.filter { info ->
            info.signatureMatches && !info.downloadUrl.isNullOrBlank()
        }
        return Partition(
            installable = installable,
            skippedCount = candidates.size - installable.size,
        )
    }
}

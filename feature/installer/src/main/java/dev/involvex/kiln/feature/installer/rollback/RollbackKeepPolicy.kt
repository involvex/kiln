package dev.involvex.kiln.feature.installer.rollback

/**
 * Pure helper: which versionCodes to drop when keeping the newest [keep] APKs.
 * [sortedNewestFirst] must already be sorted descending by versionCode.
 */
object RollbackKeepPolicy {
    fun versionCodesToDelete(sortedNewestFirst: List<Long>, keep: Int): List<Long> {
        val safeKeep = keep.coerceAtLeast(0)
        return sortedNewestFirst.drop(safeKeep)
    }
}

package dev.involvex.kiln.feature.sources.updatecheck

import dev.involvex.kiln.core.model.AppUpdateInfo

/**
 * Pure policy: which updates may be auto-enqueued from a background check.
 * Never includes signature mismatches / WARNUNG rows (`signatureMatches == false`).
 */
object AutoInstallPolicy {
    fun selectForEnqueue(updates: Collection<AppUpdateInfo>, autoInstallMatching: Boolean): List<AppUpdateInfo> {
        if (!autoInstallMatching) return emptyList()
        return updates.filter { it.signatureMatches }
    }
}

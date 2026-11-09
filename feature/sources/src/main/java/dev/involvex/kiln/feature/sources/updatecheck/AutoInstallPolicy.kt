package dev.involvex.kiln.feature.sources.updatecheck

import dev.involvex.kiln.core.model.AppUpdateInfo
import java.util.concurrent.TimeUnit
import timber.log.Timber

/**
 * Pure policy: which updates may be auto-enqueued from a background check.
 * Never includes signature mismatches / WARNUNG rows (`signatureMatches == false`).
 * Skips major version bumps and respects first-seen cooldown when configured.
 */
object AutoInstallPolicy {
    fun selectForEnqueue(
        updates: Collection<AppUpdateInfo>,
        autoInstallMatching: Boolean,
        installedVersionNames: Map<String, String?> = emptyMap(),
        firstSeenEpochMs: Map<String, Long> = emptyMap(),
        cooldownDays: Int = 0,
        nowEpochMs: Long = System.currentTimeMillis(),
    ): List<AppUpdateInfo> {
        if (!autoInstallMatching) return emptyList()
        val cooldownMs = if (cooldownDays > 0) {
            TimeUnit.DAYS.toMillis(cooldownDays.toLong())
        } else {
            0L
        }
        return updates.filter { update ->
            if (update.watchOnly) return@filter false
            if (!update.signatureMatches) return@filter false
            val installedName = installedVersionNames[update.packageName]
            if (isMajorBump(installedName, update.availableVersionName)) {
                Timber.d(
                    "Auto-install skipped (major bump): %s %s → %s",
                    update.packageName,
                    installedName,
                    update.availableVersionName,
                )
                return@filter false
            }
            if (cooldownMs > 0) {
                val key = "${update.packageName}:${update.availableVersionCode}"
                val firstSeen = firstSeenEpochMs[key]
                if (firstSeen == null) {
                    Timber.d("Auto-install skipped (no first-seen): %s", update.packageName)
                    return@filter false
                }
                if (nowEpochMs - firstSeen < cooldownMs) {
                    Timber.d(
                        "Auto-install skipped (cooldown): %s firstSeen=%d cooldownDays=%d",
                        update.packageName,
                        firstSeen,
                        cooldownDays,
                    )
                    return@filter false
                }
            }
            true
        }
    }

    /**
     * Major bump = first numeric segment of available versionName is greater than installed.
     * If either side lacks a parseable major, treat as non-major (do not invent).
     */
    fun isMajorBump(installedVersionName: String?, availableVersionName: String): Boolean {
        val installedMajor = majorComponent(installedVersionName) ?: return false
        val availableMajor = majorComponent(availableVersionName) ?: return false
        return availableMajor > installedMajor
    }

    fun majorComponent(versionName: String?): Int? {
        if (versionName.isNullOrBlank()) return null
        val match = MAJOR_REGEX.find(versionName.trim()) ?: return null
        return match.groupValues[1].toIntOrNull()
    }

    private val MAJOR_REGEX = Regex("""^(\d+)""")
}

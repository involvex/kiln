package dev.involvex.kiln.feature.settings.config

/**
 * Pure merge preview helpers for unit tests (no Android / Room).
 */
object KilnConfigMerge {
    fun countRepoChanges(existingIds: Set<String>, incoming: List<KilnConfigRepoV1>): Pair<Int, Int> {
        var neu = 0
        var updated = 0
        for (repo in incoming) {
            if (repo.id.isBlank()) continue
            if (repo.id in existingIds) updated++ else neu++
        }
        return neu to updated
    }

    fun prefsSectionsPresent(prefs: KilnConfigPreferencesV1): List<String> = buildList {
        if (prefs.backgroundUpdates != null) add("backgroundUpdates")
        if (prefs.automation != null) add("automation")
        if (prefs.maintenance != null) add("maintenance")
        if (prefs.appearance != null) add("appearance")
        if (prefs.installer != null) add("installer")
        if (prefs.termux != null) add("termux")
        if (prefs.network != null) add("network")
    }
}

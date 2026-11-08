package dev.involvex.kiln.core.model

import kotlinx.coroutines.flow.Flow

/**
 * User preferences for M8 background update checks.
 * Defaults: disabled, Wi‑Fi/unmetered only, notify-only (no auto-install).
 */
data class BackgroundUpdatePrefs(val enabled: Boolean = false, val unmeteredOnly: Boolean = true, val autoInstallMatching: Boolean = false)

/**
 * Persisted prefs (DataStore). Implemented in `:feature:settings`, bound in `:app`.
 */
interface BackgroundUpdatePreferences {
    fun observe(): Flow<BackgroundUpdatePrefs>

    suspend fun current(): BackgroundUpdatePrefs

    suspend fun setEnabled(enabled: Boolean)

    suspend fun setUnmeteredOnly(unmeteredOnly: Boolean)

    suspend fun setAutoInstallMatching(autoInstallMatching: Boolean)
}

/**
 * Schedules / cancels WorkManager periodic + one-shot checks.
 * Implemented in `:feature:sources`, bound in `:app`.
 */
interface BackgroundUpdateScheduler {
    /** Apply [prefs]: schedule when enabled, cancel when disabled. */
    fun apply(prefs: BackgroundUpdatePrefs)

    /** One-shot expedited check for Settings “Check now”. */
    fun enqueueImmediate()
}

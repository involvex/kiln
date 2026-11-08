package dev.involvex.kiln.core.model

import kotlinx.coroutines.flow.Flow

/**
 * User preferences for background update checks.
 * Defaults: disabled, Wi‑Fi/unmetered only, notify-only (no auto-install), no cooldown.
 */
data class BackgroundUpdatePrefs(
    val enabled: Boolean = false,
    val unmeteredOnly: Boolean = true,
    val autoInstallMatching: Boolean = false,
    /** Days to wait after Kiln first sees an update before auto-install. 0 = off. */
    val updateCooldownDays: Int = 0,
)

/**
 * Persisted prefs (DataStore). Implemented in `:feature:settings`, bound in `:app`.
 */
interface BackgroundUpdatePreferences {
    fun observe(): Flow<BackgroundUpdatePrefs>

    suspend fun current(): BackgroundUpdatePrefs

    suspend fun setEnabled(enabled: Boolean)

    suspend fun setUnmeteredOnly(unmeteredOnly: Boolean)

    suspend fun setAutoInstallMatching(autoInstallMatching: Boolean)

    suspend fun setUpdateCooldownDays(days: Int)
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

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
    /** Periodic check interval in hours. Only 12, 24, or 168. */
    val checkIntervalHours: Int = DEFAULT_CHECK_INTERVAL_HOURS,
) {
    companion object {
        const val DEFAULT_CHECK_INTERVAL_HOURS: Int = 12
        val ALLOWED_CHECK_INTERVAL_HOURS: Set<Int> = setOf(12, 24, 168)

        fun sanitizeIntervalHours(hours: Int): Int = if (hours in ALLOWED_CHECK_INTERVAL_HOURS) hours else DEFAULT_CHECK_INTERVAL_HOURS
    }
}

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

    suspend fun setCheckIntervalHours(hours: Int)
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

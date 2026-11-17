package dev.involvex.kiln.feature.widgets

import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.core.model.PrivilegedExecutor
import dev.involvex.kiln.core.model.ProcessStatsGate
import dev.involvex.kiln.core.model.WidgetPreferences
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class ProcessWidgetRefresher @Inject constructor(
    private val privilegedExecutor: PrivilegedExecutor,
    private val widgetPreferences: WidgetPreferences,
    private val processWidgetStore: ProcessWidgetStore,
) {
    /**
     * Polls allowlisted process stats when capability is present.
     * @return true if a shell poll ran successfully with rows (may be empty)
     */
    suspend fun refreshIfStale(force: Boolean = false): Boolean {
        val caps = privilegedExecutor.currentStatus().capabilities
        if (!ProcessStatsGate.canList(caps)) {
            processWidgetStore.clear()
            return false
        }
        val now = System.currentTimeMillis()
        val last = widgetPreferences.current().lastProcessRefreshEpochMs
        if (!force && now - last < MIN_INTERVAL_MS) {
            return false
        }
        return when (val result = privilegedExecutor.listProcessStats(LIMIT)) {
            is Result.Failure -> {
                Timber.w("Process widget refresh failed: %s", result.message)
                processWidgetStore.publishFailure(
                    errorKey = result.message.substringBefore(':').ifBlank { "shell_error" },
                )
                false
            }

            is Result.Success -> {
                processWidgetStore.publish(result.value)
                widgetPreferences.setLastProcessRefreshEpochMs(now)
                true
            }
        }
    }

    companion object {
        const val MIN_INTERVAL_MS = 15_000L
        const val LIMIT = 6
    }
}

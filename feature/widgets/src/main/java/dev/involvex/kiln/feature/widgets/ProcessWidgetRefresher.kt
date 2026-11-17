package dev.involvex.kiln.feature.widgets

import dev.involvex.kiln.core.common.IoDispatcher
import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.core.model.PrivilegedExecutor
import dev.involvex.kiln.core.model.ProcessStatsGate
import dev.involvex.kiln.core.model.WidgetPreferences
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

@Singleton
class ProcessWidgetRefresher @Inject constructor(
    private val privilegedExecutor: PrivilegedExecutor,
    private val widgetPreferences: WidgetPreferences,
    private val processWidgetStore: ProcessWidgetStore,
    private val surfaceUpdater: WidgetSurfaceUpdater,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val mutex = Mutex()

    /**
     * Non-blocking refresh for Glance bind / BroadcastReceiver.
     * Never await dumpsys on the widget or receiver critical path (ANR).
     */
    fun enqueueRefresh(force: Boolean = false) {
        scope.launch {
            mutex.withLock {
                val ran = refreshIfStale(force = force)
                if (ran || force) {
                    runCatching { surfaceUpdater.refreshProcesses() }
                }
            }
        }
    }

    /**
     * Polls allowlisted process stats when capability is present.
     * @return true if a shell poll ran (success or empty success)
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
                // Still advance throttle so a failing dump cannot ANR-loop every bind.
                widgetPreferences.setLastProcessRefreshEpochMs(now)
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

        /** Dense table rows; keep modest to limit label resolution work. */
        const val LIMIT = 8
    }
}

package dev.involvex.kiln.core.model

import kotlinx.coroutines.flow.Flow

data class DevPrefs(
    /** When true, Timber DebugTree is planted (also on non-debuggable builds). */
    val debugLogEnabled: Boolean = false,
)

interface DevPreferences {
    fun observe(): Flow<DevPrefs>

    suspend fun current(): DevPrefs

    suspend fun setDebugLogEnabled(enabled: Boolean)
}

/**
 * Applies [DevPrefs.debugLogEnabled] to the Timber forest at runtime.
 * Implemented in `:app`.
 */
interface DebugLogController {
    fun apply(enabled: Boolean)
}

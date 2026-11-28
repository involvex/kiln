package dev.involvex.kiln.core.model

import kotlinx.coroutines.flow.Flow

/**
 * Magisk / libsu root opt-in (default off). Implementation lives in `:core:privilege`.
 * Features must never call `su` / libsu directly — use [PrivilegedExecutor].
 */
interface RootPreferences {
    fun observeEnabled(): Flow<Boolean>

    /** Synchronous read for status publishing (cached). */
    fun isEnabled(): Boolean

    fun setEnabled(enabled: Boolean)
}

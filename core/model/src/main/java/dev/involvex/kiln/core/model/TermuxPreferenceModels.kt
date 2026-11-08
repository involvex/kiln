package dev.involvex.kiln.core.model

import kotlinx.coroutines.flow.Flow

/**
 * Feature-flagged Termux RUN_COMMAND integration preferences (PLAN §4.7).
 *
 * [packageId] is the Termux (or fork) applicationId so action/extra keys and
 * service class names match that build's TERMUX_PACKAGE_NAME.
 */
data class TermuxPrefs(
    /** When false, Kiln does not start Termux commands (feature flag). */
    val enabled: Boolean = false,
    /** Termux applicationId; default is the official F-Droid/GitHub package. */
    val packageId: String = DEFAULT_TERMUX_PACKAGE_ID,
) {
    companion object {
        const val DEFAULT_TERMUX_PACKAGE_ID: String = "com.termux"
    }
}

interface TermuxPreferences {
    fun observe(): Flow<TermuxPrefs>

    suspend fun current(): TermuxPrefs

    suspend fun setEnabled(enabled: Boolean)

    suspend fun setPackageId(packageId: String)
}

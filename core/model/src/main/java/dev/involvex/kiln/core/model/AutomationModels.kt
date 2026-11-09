package dev.involvex.kiln.core.model

import kotlinx.coroutines.flow.Flow

/** Opt-in Intent/Broadcast automation API (PLAN §12). Off by default. */
data class AutomationPrefs(val enabled: Boolean = false)

interface AutomationPreferences {
    fun observe(): Flow<AutomationPrefs>

    suspend fun current(): AutomationPrefs

    suspend fun setEnabled(enabled: Boolean)
}

/** Stable action / extra names for Termux, Tasker, and `am broadcast`. */
object AutomationApi {
    const val PERMISSION = "dev.involvex.kiln.permission.AUTOMATION"

    const val ACTION_CHECK_UPDATES = "dev.involvex.kiln.action.CHECK_UPDATES"
    const val ACTION_INSTALL_UPDATE = "dev.involvex.kiln.action.INSTALL_UPDATE"
    const val ACTION_INSTALL_FROM_URL = "dev.involvex.kiln.action.INSTALL_FROM_URL"
    const val ACTION_CLEAN_CACHE = "dev.involvex.kiln.action.CLEAN_CACHE"
    const val ACTION_RESULT = "dev.involvex.kiln.action.AUTOMATION_RESULT"

    const val EXTRA_PACKAGE_NAME = "packageName"
    const val EXTRA_URL = "url"
    const val EXTRA_VERSION_NAME = "versionName"
    const val EXTRA_VERSION_CODE = "versionCode"
    const val EXTRA_SHA256 = "sha256"
    const val EXTRA_REQUEST_ACTION = "requestAction"
    const val EXTRA_OK = "ok"
    const val EXTRA_MESSAGE = "message"
}

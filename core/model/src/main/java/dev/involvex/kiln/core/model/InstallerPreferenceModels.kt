package dev.involvex.kiln.core.model

import kotlinx.coroutines.flow.Flow

/**
 * How Kiln prefers to commit APK installs.
 * [SYSTEM] always uses PackageInstaller (system confirm).
 * [SHIZUKU] uses silent `pm install` when [PrivilegeCapability.SILENT_INSTALL] is available,
 * otherwise falls back to PackageInstaller.
 */
enum class PreferredInstaller {
    SYSTEM,
    SHIZUKU,
}

interface InstallerPreferences {
    fun observe(): Flow<PreferredInstaller>

    suspend fun current(): PreferredInstaller

    suspend fun setPreferred(preferred: PreferredInstaller)
}

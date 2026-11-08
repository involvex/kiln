package dev.involvex.kiln.feature.settings

import dev.involvex.kiln.core.model.AppearancePrefs
import dev.involvex.kiln.core.model.BackgroundUpdatePrefs
import dev.involvex.kiln.core.model.DevPrefs
import dev.involvex.kiln.core.model.PreferredInstaller

data class SettingsUiState(
    val title: String = "",
    val description: String = "",
    val backgroundUpdates: BackgroundUpdatePrefs = BackgroundUpdatePrefs(),
    val appearance: AppearancePrefs = AppearancePrefs(),
    val preferredInstaller: PreferredInstaller = PreferredInstaller.SYSTEM,
    val shizukuSilentAvailable: Boolean = false,
    val devPrefs: DevPrefs = DevPrefs(),
    val checkNowQueued: Boolean = false,
)

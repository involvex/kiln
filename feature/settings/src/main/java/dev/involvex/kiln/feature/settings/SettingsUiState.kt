package dev.involvex.kiln.feature.settings

import dev.involvex.kiln.core.model.BackgroundUpdatePrefs

data class SettingsUiState(
    val title: String = "",
    val description: String = "",
    val backgroundUpdates: BackgroundUpdatePrefs = BackgroundUpdatePrefs(),
    val checkNowQueued: Boolean = false,
)

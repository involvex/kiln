package dev.involvex.kiln.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.involvex.kiln.core.model.AppearancePreferences
import dev.involvex.kiln.core.model.BackgroundUpdatePreferences
import dev.involvex.kiln.core.model.BackgroundUpdateScheduler
import dev.involvex.kiln.core.model.DebugLogController
import dev.involvex.kiln.core.model.DevPreferences
import dev.involvex.kiln.core.model.InstallerPreferences
import dev.involvex.kiln.core.model.KilnLanguage
import dev.involvex.kiln.core.model.KilnThemeStyle
import dev.involvex.kiln.core.model.PreferredInstaller
import dev.involvex.kiln.core.model.PrivilegeCapability
import dev.involvex.kiln.core.model.PrivilegedExecutor
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: BackgroundUpdatePreferences,
    private val appearancePreferences: AppearancePreferences,
    private val installerPreferences: InstallerPreferences,
    private val devPreferences: DevPreferences,
    private val debugLogController: DebugLogController,
    private val privilegedExecutor: PrivilegedExecutor,
    private val scheduler: BackgroundUpdateScheduler,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferences.observe().collect { prefs ->
                _uiState.update { it.copy(backgroundUpdates = prefs) }
                scheduler.apply(prefs)
            }
        }
        viewModelScope.launch {
            appearancePreferences.observe().collect { prefs ->
                _uiState.update { it.copy(appearance = prefs) }
            }
        }
        viewModelScope.launch {
            installerPreferences.observe().collect { preferred ->
                _uiState.update { it.copy(preferredInstaller = preferred) }
            }
        }
        viewModelScope.launch {
            devPreferences.observe().collect { prefs ->
                _uiState.update { it.copy(devPrefs = prefs) }
                debugLogController.apply(prefs.debugLogEnabled)
            }
        }
        viewModelScope.launch {
            privilegedExecutor.observeStatus().collect { status ->
                _uiState.update {
                    it.copy(
                        shizukuSilentAvailable = PrivilegeCapability.SILENT_INSTALL in status.capabilities,
                    )
                }
            }
        }
    }

    fun onScreenShown(title: String, description: String) {
        _uiState.update { it.copy(title = title, description = description) }
    }

    fun setBackgroundUpdatesEnabled(enabled: Boolean) {
        viewModelScope.launch { preferences.setEnabled(enabled) }
    }

    fun setUnmeteredOnly(unmeteredOnly: Boolean) {
        viewModelScope.launch { preferences.setUnmeteredOnly(unmeteredOnly) }
    }

    fun setAutoInstallMatching(autoInstall: Boolean) {
        viewModelScope.launch { preferences.setAutoInstallMatching(autoInstall) }
    }

    fun setUpdateCooldownDays(days: Int) {
        viewModelScope.launch { preferences.setUpdateCooldownDays(days) }
    }

    fun setTheme(theme: KilnThemeStyle) {
        viewModelScope.launch { appearancePreferences.setTheme(theme) }
    }

    fun setLanguage(language: KilnLanguage) {
        viewModelScope.launch { appearancePreferences.setLanguage(language) }
    }

    fun setPreferredInstaller(preferred: PreferredInstaller) {
        viewModelScope.launch { installerPreferences.setPreferred(preferred) }
    }

    fun setDebugLogEnabled(enabled: Boolean) {
        viewModelScope.launch { devPreferences.setDebugLogEnabled(enabled) }
    }

    fun checkNow() {
        scheduler.enqueueImmediate()
        _uiState.update { it.copy(checkNowQueued = true) }
    }

    fun consumeCheckNowQueued() {
        _uiState.update { it.copy(checkNowQueued = false) }
    }
}

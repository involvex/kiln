package dev.involvex.kiln.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.involvex.kiln.core.model.BackgroundUpdatePreferences
import dev.involvex.kiln.core.model.BackgroundUpdateScheduler
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: BackgroundUpdatePreferences,
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

    fun checkNow() {
        scheduler.enqueueImmediate()
        _uiState.update { it.copy(checkNowQueued = true) }
    }

    fun consumeCheckNowQueued() {
        _uiState.update { it.copy(checkNowQueued = false) }
    }
}

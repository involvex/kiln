package dev.involvex.kiln.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.involvex.kiln.core.common.IoDispatcher
import dev.involvex.kiln.core.model.AppUpdateRepository
import dev.involvex.kiln.core.model.InstalledAppsReader
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class IgnoredUpdateApp(val packageName: String, val label: String)

data class SettingsIgnoredUpdatesUiState(val apps: List<IgnoredUpdateApp> = emptyList())

@HiltViewModel
class SettingsIgnoredUpdatesViewModel @Inject constructor(
    private val appUpdateRepository: AppUpdateRepository,
    private val installedAppsReader: InstalledAppsReader,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsIgnoredUpdatesUiState())
    val uiState: StateFlow<SettingsIgnoredUpdatesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            appUpdateRepository.observeIgnoredUpdatePackages().collect { packages ->
                val labelMap = withContext(ioDispatcher) {
                    installedAppsReader.userApps().associate { it.packageName to it.label }
                }
                _uiState.update {
                    it.copy(
                        apps = packages.map { pkg ->
                            IgnoredUpdateApp(packageName = pkg, label = labelMap[pkg] ?: pkg)
                        },
                    )
                }
            }
        }
    }

    fun reenable(packageName: String) {
        viewModelScope.launch {
            appUpdateRepository.setIgnoreUpdates(packageName, ignoreUpdates = false)
        }
    }
}

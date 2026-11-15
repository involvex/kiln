package dev.involvex.kiln.feature.permissions.background

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.core.model.AutostartListFilter
import dev.involvex.kiln.core.model.BackgroundControlsApp
import dev.involvex.kiln.core.model.BackgroundControlsGate
import dev.involvex.kiln.core.model.BatteryUsageMode
import dev.involvex.kiln.core.model.PrivilegedExecutor
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class BackgroundControlsViewModel @Inject constructor(
    private val repository: BackgroundControlsRepository,
    private val privilegedExecutor: PrivilegedExecutor,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        BackgroundControlsUiState(
            isHyperOs = repository.isHyperOs(),
            canMutate = repository.canMutate(),
        ),
    )
    val uiState: StateFlow<BackgroundControlsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            privilegedExecutor.observeStatus().collect { status ->
                _uiState.update {
                    it.copy(canMutate = BackgroundControlsGate.canMutate(status.capabilities))
                }
            }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val includeSystem = _uiState.value.includeSystem
            _uiState.update { it.copy(isLoading = true, message = null) }
            val apps = repository.loadApps(includeSystem)
            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    apps = apps,
                    filteredApps = filterApps(apps, state.query, state.autostartFilter, state.isHyperOs),
                    isHyperOs = repository.isHyperOs(),
                    canMutate = repository.canMutate(),
                )
            }
        }
    }

    fun setQuery(query: String) {
        _uiState.update { state ->
            state.copy(
                query = query,
                filteredApps = filterApps(state.apps, query, state.autostartFilter, state.isHyperOs),
            )
        }
    }

    fun setIncludeSystem(include: Boolean) {
        if (_uiState.value.includeSystem == include) return
        _uiState.update { it.copy(includeSystem = include) }
        refresh()
    }

    fun setAutostartFilter(filter: AutostartListFilter) {
        _uiState.update { state ->
            state.copy(
                autostartFilter = filter,
                filteredApps = filterApps(state.apps, state.query, filter, state.isHyperOs),
            )
        }
    }

    fun setAutostart(packageName: String, allowed: Boolean) {
        if (!_uiState.value.canMutate || !_uiState.value.isHyperOs) return
        viewModelScope.launch {
            _uiState.update { it.copy(mutatingPackage = packageName, message = null) }
            when (val result = repository.setAutostart(packageName, allowed)) {
                is Result.Failure -> _uiState.update {
                    it.copy(
                        mutatingPackage = null,
                        message = result.message,
                        messageIsError = true,
                    )
                }

                is Result.Success -> {
                    patchApp(packageName) { it.copy(autostartAllowed = allowed) }
                    _uiState.update { it.copy(mutatingPackage = null) }
                }
            }
        }
    }

    fun setBatteryMode(packageName: String, mode: BatteryUsageMode) {
        if (!_uiState.value.canMutate) return
        viewModelScope.launch {
            _uiState.update { it.copy(mutatingPackage = packageName, message = null) }
            when (val result = repository.setBatteryMode(packageName, mode)) {
                is Result.Failure -> _uiState.update {
                    it.copy(
                        mutatingPackage = null,
                        message = result.message,
                        messageIsError = true,
                    )
                }

                is Result.Success -> {
                    patchApp(packageName) { it.copy(batteryMode = mode) }
                    _uiState.update { it.copy(mutatingPackage = null) }
                }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null, messageIsError = false) }
    }

    private fun patchApp(packageName: String, transform: (BackgroundControlsApp) -> BackgroundControlsApp) {
        _uiState.update { state ->
            val apps = state.apps.map { if (it.packageName == packageName) transform(it) else it }
            state.copy(
                apps = apps,
                filteredApps = filterApps(apps, state.query, state.autostartFilter, state.isHyperOs),
            )
        }
    }

    companion object {
        internal fun filterApps(
            apps: List<BackgroundControlsApp>,
            query: String,
            autostartFilter: AutostartListFilter,
            isHyperOs: Boolean,
        ): List<BackgroundControlsApp> {
            val q = query.trim()
            return apps.asSequence()
                .filter { app ->
                    q.isEmpty() ||
                        app.label.contains(q, ignoreCase = true) ||
                        app.packageName.contains(q, ignoreCase = true)
                }
                .filter { app ->
                    if (!isHyperOs || autostartFilter == AutostartListFilter.ALL) {
                        true
                    } else {
                        when (autostartFilter) {
                            AutostartListFilter.ENABLED -> app.autostartAllowed == true
                            AutostartListFilter.DISABLED -> app.autostartAllowed == false
                            AutostartListFilter.ALL -> true
                        }
                    }
                }
                .toList()
        }
    }
}

package dev.involvex.kiln.feature.sources.discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.core.model.ExodusPrivacyPreferences
import dev.involvex.kiln.core.model.ExodusReportRepository
import dev.involvex.kiln.core.model.WatchlistRepository
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CatalogDetailUiState(
    val isLoading: Boolean = true,
    val row: DiscoveryAppRow? = null,
    val isWatching: Boolean = false,
    val message: String? = null,
    val messageIsError: Boolean = false,
    val exodusTrackerNames: List<String> = emptyList(),
    val exodusApiConfigured: Boolean = false,
    val exodusLoading: Boolean = false,
)

@HiltViewModel
class CatalogDetailViewModel @Inject constructor(
    private val repository: DiscoveryRepository,
    private val watchlistRepository: WatchlistRepository,
    private val exodusReportRepository: ExodusReportRepository,
    private val exodusPrivacyPreferences: ExodusPrivacyPreferences,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CatalogDetailUiState())
    val uiState: StateFlow<CatalogDetailUiState> = _uiState.asStateFlow()
    private var watchJob: Job? = null
    private var exodusJob: Job? = null

    fun load(packageName: String, repoId: String) {
        watchJob?.cancel()
        exodusJob?.cancel()
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null, exodusTrackerNames = emptyList()) }
            val row = repository.catalogRow(packageName, repoId)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    row = row,
                    message = if (row == null) "catalog_missing" else null,
                    messageIsError = row == null,
                )
            }
            if (row != null) {
                loadExodus(row.packageName)
            }
        }
        watchJob = viewModelScope.launch {
            watchlistRepository.observeIsWatching(packageName).collect { watching ->
                _uiState.update { it.copy(isWatching = watching) }
            }
        }
    }

    private fun loadExodus(packageName: String) {
        exodusJob?.cancel()
        exodusJob = viewModelScope.launch {
            val configured = exodusPrivacyPreferences.current().isApiConfigured()
            _uiState.update { it.copy(exodusApiConfigured = configured, exodusLoading = configured) }
            if (!configured) {
                _uiState.update { it.copy(exodusLoading = false, exodusTrackerNames = emptyList()) }
                return@launch
            }
            when (val result = exodusReportRepository.reportFor(packageName)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            exodusLoading = false,
                            exodusTrackerNames = result.value.trackerNames,
                        )
                    }
                }

                is Result.Failure -> {
                    _uiState.update { it.copy(exodusLoading = false, exodusTrackerNames = emptyList()) }
                }
            }
        }
    }

    fun install() {
        val row = _uiState.value.row ?: return
        viewModelScope.launch {
            when (val result = repository.install(row)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(message = "install_queued:${row.name}", messageIsError = false)
                    }
                }

                is Result.Failure -> {
                    _uiState.update {
                        it.copy(message = result.message, messageIsError = true)
                    }
                }
            }
        }
    }

    fun toggleWatch() {
        val row = _uiState.value.row ?: return
        val wasWatching = _uiState.value.isWatching
        viewModelScope.launch {
            val result = if (wasWatching) {
                watchlistRepository.unwatchPackage(row.packageName)
            } else {
                watchlistRepository.watchCatalogPackage(row.packageName, row.repoId)
            }
            when (result) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            message = if (wasWatching) "unwatch_ok" else "watch_ok",
                            messageIsError = false,
                        )
                    }
                }

                is Result.Failure -> {
                    _uiState.update {
                        it.copy(
                            message = result.message,
                            messageIsError = true,
                        )
                    }
                }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}

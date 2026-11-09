package dev.involvex.kiln.feature.sources.discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.involvex.kiln.core.common.Result
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TrendingUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val shelves: List<TrendingShelf> = emptyList(),
    val message: String? = null,
    val messageIsError: Boolean = false,
)

@HiltViewModel
class TrendingViewModel @Inject constructor(
    private val trendingRepository: TrendingRepository,
    private val discoveryRepository: DiscoveryRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TrendingUiState())
    val uiState: StateFlow<TrendingUiState> = _uiState.asStateFlow()

    init {
        refreshLocal()
    }

    /** Pull-to-refresh: sync F-Droid catalogs, then rebuild shelves. */
    fun refreshCatalog() {
        if (_uiState.value.isRefreshing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, message = null) }
            when (val result = discoveryRepository.refreshCatalog()) {
                is Result.Success -> {
                    val shelves = trendingRepository.loadShelves()
                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            isLoading = false,
                            shelves = shelves,
                            message = "catalog_ok:${result.value}",
                            messageIsError = false,
                        )
                    }
                }

                is Result.Failure -> {
                    val shelves = trendingRepository.loadShelves()
                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            isLoading = false,
                            shelves = shelves,
                            message = result.message,
                            messageIsError = true,
                        )
                    }
                }
            }
        }
    }

    fun refreshLocal() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val shelves = trendingRepository.loadShelves()
            _uiState.update { it.copy(isLoading = false, shelves = shelves) }
        }
    }

    fun install(row: DiscoveryAppRow) {
        viewModelScope.launch {
            when (val result = discoveryRepository.install(row)) {
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

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}

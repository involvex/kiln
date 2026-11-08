package dev.involvex.kiln.feature.sources.discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.involvex.kiln.core.common.Result
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class DiscoveryViewModel @Inject constructor(private val repository: DiscoveryRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(DiscoveryUiState())
    val uiState: StateFlow<DiscoveryUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        refreshLocal()
    }

    fun setQuery(query: String) {
        _uiState.update { it.copy(query = query) }
        scheduleSearch()
    }

    fun setCategory(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
        scheduleSearch()
    }

    fun refreshCatalog() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, message = null) }
            when (val result = repository.refreshCatalog()) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            message = "catalog_ok:${result.value}",
                            messageIsError = false,
                        )
                    }
                    refreshLocal()
                }

                is Result.Failure -> {
                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            message = result.message,
                            messageIsError = true,
                        )
                    }
                }
            }
        }
    }

    fun install(row: DiscoveryAppRow) {
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

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun scheduleSearch() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            runSearch()
        }
    }

    private fun refreshLocal() {
        viewModelScope.launch {
            val count = repository.packageCount()
            val categories = repository.categories()
            _uiState.update { it.copy(packageCount = count, categories = categories) }
            runSearch()
        }
    }

    private suspend fun runSearch() {
        val state = _uiState.value
        _uiState.update { it.copy(isSearching = true) }
        val rows = repository.search(state.query, state.selectedCategory)
        _uiState.update { it.copy(results = rows, isSearching = false) }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 250L
    }
}

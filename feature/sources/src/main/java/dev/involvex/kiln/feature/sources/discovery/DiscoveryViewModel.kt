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
        refreshSavedGithub()
    }

    fun setSourceTab(tab: DiscoverySourceTab) {
        _uiState.update { it.copy(sourceTab = tab, message = null) }
        if (tab == DiscoverySourceTab.GITHUB) {
            refreshSavedGithub()
        }
    }

    fun setQuery(query: String) {
        _uiState.update { it.copy(query = query) }
        scheduleSearch()
    }

    fun setCategory(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
        scheduleSearch()
    }

    fun setGithubQuery(query: String) {
        _uiState.update { it.copy(githubQuery = query) }
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

    fun lookupGithub() {
        val query = _uiState.value.githubQuery
        viewModelScope.launch {
            _uiState.update { it.copy(isLookingUpGithub = true, message = null) }
            when (val result = repository.lookupGithub(query)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isLookingUpGithub = false,
                            githubResults = listOf(result.value),
                            message = "github_lookup_ok:${result.value.ownerRepo}",
                            messageIsError = false,
                        )
                    }
                    refreshSavedGithub()
                }

                is Result.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLookingUpGithub = false,
                            githubResults = emptyList(),
                            message = result.message,
                            messageIsError = true,
                        )
                    }
                }
            }
        }
    }

    fun lookupSavedGithub(row: DiscoveryGithubRow) {
        _uiState.update { it.copy(githubQuery = row.ownerRepo) }
        lookupGithub()
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

    fun installGithub(row: DiscoveryGithubRow) {
        viewModelScope.launch {
            val ready = if (row.downloadUrl != null) {
                Result.success(row)
            } else {
                repository.lookupGithub(row.ownerRepo)
            }
            when (ready) {
                is Result.Failure -> {
                    _uiState.update {
                        it.copy(message = ready.message, messageIsError = true)
                    }
                }

                is Result.Success -> {
                    when (val result = repository.installGithub(ready.value)) {
                        is Result.Success -> {
                            _uiState.update {
                                it.copy(
                                    githubResults = listOf(ready.value),
                                    message = "install_queued:${ready.value.ownerRepo}",
                                    messageIsError = false,
                                )
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

    private fun refreshSavedGithub() {
        viewModelScope.launch {
            val saved = repository.savedGithubSources()
            _uiState.update { it.copy(savedGithub = saved) }
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

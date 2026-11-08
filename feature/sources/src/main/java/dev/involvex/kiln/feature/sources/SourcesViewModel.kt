package dev.involvex.kiln.feature.sources

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel
class SourcesViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(SourcesUiState())
    val uiState: StateFlow<SourcesUiState> = _uiState.asStateFlow()

    fun onScreenShown(title: String, description: String) {
        _uiState.update { it.copy(title = title, description = description) }
    }
}

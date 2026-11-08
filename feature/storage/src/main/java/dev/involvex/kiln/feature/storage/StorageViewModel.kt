package dev.involvex.kiln.feature.storage

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel
class StorageViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(StorageUiState())
    val uiState: StateFlow<StorageUiState> = _uiState.asStateFlow()

    fun onScreenShown(title: String, description: String) {
        _uiState.update { it.copy(title = title, description = description) }
    }
}

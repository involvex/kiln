package dev.involvex.kiln.feature.apps.apktools

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ApkToolsViewModel @Inject constructor(private val repository: ApkToolsRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(ApkToolsUiState())
    val uiState: StateFlow<ApkToolsUiState> = _uiState.asStateFlow()

    fun setMode(mode: ApkToolsMode) {
        _uiState.update {
            it.copy(
                mode = mode,
                message = null,
                inspect = if (mode == ApkToolsMode.INSPECT) it.inspect else it.inspect,
                compare = if (mode == ApkToolsMode.COMPARE) it.compare else it.compare,
            )
        }
    }

    fun setComparePackageQuery(value: String) {
        _uiState.update { it.copy(comparePackageQuery = value) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null, messageIsError = false) }
    }

    fun inspect(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, message = null) }
            when (val result = repository.inspectUri(uri)) {
                is dev.involvex.kiln.core.common.Result.Success ->
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            inspect = result.value,
                            message = result.value.errorCode,
                            messageIsError = result.value.errorCode != null,
                        )
                    }

                is dev.involvex.kiln.core.common.Result.Failure ->
                    _uiState.update {
                        it.copy(isBusy = false, message = result.message, messageIsError = true)
                    }
            }
        }
    }

    fun compare(left: Uri, right: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, message = null) }
            when (val result = repository.compareUris(left, right)) {
                is dev.involvex.kiln.core.common.Result.Success ->
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            compare = result.value,
                            message = result.value.errorCode,
                            messageIsError = result.value.errorCode != null,
                        )
                    }

                is dev.involvex.kiln.core.common.Result.Failure ->
                    _uiState.update {
                        it.copy(isBusy = false, message = result.message, messageIsError = true)
                    }
            }
        }
    }

    fun compareToInstalled(uri: Uri) {
        val pkg = _uiState.value.comparePackageQuery.trim()
        if (pkg.isEmpty()) {
            _uiState.update {
                it.copy(message = "package_query_empty", messageIsError = true)
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, message = null) }
            when (val result = repository.compareUriToInstalled(uri, pkg)) {
                is dev.involvex.kiln.core.common.Result.Success ->
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            compare = result.value,
                            message = result.value.errorCode,
                            messageIsError = result.value.errorCode != null,
                        )
                    }

                is dev.involvex.kiln.core.common.Result.Failure ->
                    _uiState.update {
                        it.copy(isBusy = false, message = result.message, messageIsError = true)
                    }
            }
        }
    }
}

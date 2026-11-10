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
                messageIsError = false,
                exportCount = if (mode == ApkToolsMode.EXPORT_SPLITS) it.exportCount else null,
            )
        }
    }

    fun setComparePackageQuery(value: String) {
        _uiState.update { it.copy(comparePackageQuery = value) }
    }

    fun setResignAlias(value: String) {
        _uiState.update { it.copy(resignAlias = value) }
    }

    fun setResignConfirmed(value: Boolean) {
        _uiState.update { it.copy(resignConfirmed = value) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null, messageIsError = false) }
    }

    fun setResignApk(uri: Uri) {
        _uiState.update {
            it.copy(
                resignApkUri = uri,
                resignApkLabel = repository.displayName(uri),
                message = null,
            )
        }
    }

    fun setResignKeystore(uri: Uri) {
        _uiState.update {
            it.copy(
                resignKeystoreUri = uri,
                resignKeystoreLabel = repository.displayName(uri),
                message = null,
            )
        }
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

    /**
     * [storeSecret] / [keySecret] must come from ephemeral UI state only; never persist them.
     * Callers should clear the arrays after this returns to the UI layer.
     */
    fun resignTo(outputUri: Uri, storeSecret: CharArray, keySecret: CharArray) {
        val state = _uiState.value
        val apk = state.resignApkUri
        val keystore = state.resignKeystoreUri
        if (apk == null || keystore == null) {
            _uiState.update { it.copy(message = "resign_inputs_missing", messageIsError = true) }
            return
        }
        if (!state.resignConfirmed) {
            _uiState.update { it.copy(message = "resign_confirm_required", messageIsError = true) }
            return
        }
        if (state.resignAlias.isBlank()) {
            _uiState.update { it.copy(message = "alias_empty", messageIsError = true) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, message = null) }
            when (
                val result = repository.resign(
                    apkUri = apk,
                    keystoreUri = keystore,
                    outputUri = outputUri,
                    alias = state.resignAlias,
                    storePassword = storeSecret,
                    keyPassword = keySecret,
                )
            ) {
                is dev.involvex.kiln.core.common.Result.Success ->
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            resignConfirmed = false,
                            message = "resign_ok",
                            messageIsError = false,
                        )
                    }

                is dev.involvex.kiln.core.common.Result.Failure ->
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            message = result.message,
                            messageIsError = true,
                        )
                    }
            }
        }
    }

    fun exportSplits(bundleUri: Uri, treeUri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, message = null, exportCount = null) }
            when (val result = repository.exportSplits(bundleUri, treeUri)) {
                is dev.involvex.kiln.core.common.Result.Success ->
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            exportCount = result.value,
                            message = "export_ok",
                            messageIsError = false,
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

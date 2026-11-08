package dev.involvex.kiln.feature.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.core.model.ApkExportFormat
import dev.involvex.kiln.core.model.AppUpdateRepository
import dev.involvex.kiln.core.model.InstallQueue
import dev.involvex.kiln.feature.apps.data.InstalledAppsRepository
import dev.involvex.kiln.feature.apps.data.InstallerLabels
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = AppDetailViewModel.Factory::class)
class AppDetailViewModel @AssistedInject constructor(
    @Assisted private val packageName: String,
    private val repository: InstalledAppsRepository,
    private val appUpdateRepository: AppUpdateRepository,
    private val installQueue: InstallQueue,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AppDetailUiState())
    val uiState: StateFlow<AppDetailUiState> = _uiState.asStateFlow()

    init {
        refresh()
        viewModelScope.launch {
            appUpdateRepository.updates().collect { map ->
                _uiState.update { it.copy(availableUpdate = map[packageName]) }
            }
        }
        viewModelScope.launch {
            installQueue.jobs().collect { jobs ->
                _uiState.update { it.copy(installJob = jobs[packageName]) }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = repository.detail(packageName)) {
                is Result.Success -> {
                    val detail = result.value
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            detail = detail,
                            installerLabel = InstallerLabels.displayName(detail.installerPackage),
                            errorMessage = null,
                        )
                    }
                }

                is Result.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message,
                        )
                    }
                }
            }
        }
    }

    fun onEvent(event: AppDetailEvent) {
        when (event) {
            AppDetailEvent.Refresh -> refresh()

            AppDetailEvent.Export -> openExport()

            AppDetailEvent.DismissExport -> _uiState.update { it.copy(exportDialog = null) }

            is AppDetailEvent.FormatSelected -> {
                _uiState.update { state ->
                    state.copy(exportDialog = state.exportDialog?.copy(format = event.format))
                }
            }

            AppDetailEvent.ClearSnackbar -> _uiState.update { it.copy(snackbarMessage = null) }

            AppDetailEvent.InstallUpdate -> enqueueUpdate()

            AppDetailEvent.OpenUnknownSourcesSettings,
            AppDetailEvent.Launch,
            AppDetailEvent.OpenAppInfo,
            AppDetailEvent.Uninstall,
            -> Unit // handled in UI via intents
        }
    }

    fun setExportInProgress(inProgress: Boolean) {
        _uiState.update { it.copy(exportInProgress = inProgress) }
    }

    fun showMessage(message: String) {
        _uiState.update { it.copy(snackbarMessage = message) }
    }

    private fun enqueueUpdate() {
        val update = _uiState.value.availableUpdate ?: return
        viewModelScope.launch {
            when (val result = installQueue.enqueue(update)) {
                is Result.Success -> Unit
                is Result.Failure -> showMessage(result.message)
            }
        }
    }

    private fun openExport() {
        val detail = _uiState.value.detail ?: return
        _uiState.update {
            it.copy(
                exportDialog = ExportDialogState(
                    packageNames = listOf(detail.packageName),
                    isBatch = false,
                    warnBaseOnly = detail.hasSplits,
                    format = if (detail.hasSplits) ApkExportFormat.APKS else ApkExportFormat.APK,
                ),
            )
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(packageName: String): AppDetailViewModel
    }
}

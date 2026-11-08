package dev.involvex.kiln.feature.backup

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.core.model.PrivilegedExecutor
import dev.involvex.kiln.feature.backup.data.BackupRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class BackupViewModel @Inject constructor(private val repository: BackupRepository, private val privilegedExecutor: PrivilegedExecutor) :
    ViewModel() {
    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            privilegedExecutor.observeStatus().collect {
                _uiState.update { state -> state.copy(tier = repository.activeTier()) }
            }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null, tier = repository.activeTier()) }
            val apps = repository.listCandidates()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    apps = apps,
                    selectedPackage = it.selectedPackage?.takeIf { pkg -> apps.any { app -> app.packageName == pkg } },
                )
            }
        }
    }

    fun setQuery(query: String) = _uiState.update { it.copy(query = query) }

    fun select(packageName: String) = _uiState.update { it.copy(selectedPackage = packageName) }

    fun requestBackup() {
        val pkg = _uiState.value.selectedPackage ?: return
        _uiState.update { it.copy(pendingBackupPackage = pkg) }
    }

    fun dismissBackup() = _uiState.update { it.copy(pendingBackupPackage = null) }

    fun requestRestore() = _uiState.update { it.copy(pendingRestore = true) }

    fun dismissRestore() = _uiState.update { it.copy(pendingRestore = false) }

    fun suggestedFileName(packageName: String): String {
        val app = _uiState.value.apps.find { it.packageName == packageName }
        return repository.suggestedFileName(packageName, app?.versionName)
    }

    fun performBackup(uri: Uri, contentResolver: ContentResolver) {
        val pkg = _uiState.value.pendingBackupPackage ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true, pendingBackupPackage = null, message = null) }
            when (val result = repository.backupToUri(pkg, uri, contentResolver)) {
                is Result.Failure -> _uiState.update {
                    it.copy(busy = false, message = result.message)
                }

                is Result.Success -> _uiState.update {
                    it.copy(busy = false, message = "backup_ok:${result.value.packageName}")
                }
            }
        }
    }

    fun performRestore(uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true, pendingRestore = false, message = null) }
            when (val result = repository.restoreFromUri(uri, contentResolver)) {
                is Result.Failure -> _uiState.update {
                    it.copy(busy = false, message = result.message)
                }

                is Result.Success -> _uiState.update {
                    it.copy(busy = false, message = "restore_queued:${result.value}")
                }
            }
        }
    }

    fun consumeMessage() = _uiState.update { it.copy(message = null) }
}

package dev.involvex.kiln.feature.backup

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.core.model.BackupPreferences
import dev.involvex.kiln.core.model.BackupPrefs
import dev.involvex.kiln.core.model.PrivilegedExecutor
import dev.involvex.kiln.feature.backup.data.BackupRepository
import dev.involvex.kiln.feature.backup.data.BackupTreeStore
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val repository: BackupRepository,
    private val privilegedExecutor: PrivilegedExecutor,
    private val backupPreferences: BackupPreferences,
    private val treeStore: BackupTreeStore,
) : ViewModel() {
    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    private var pendingRestoreUri: Uri? = null
    private var pendingPassword: CharArray? = null

    init {
        viewModelScope.launch {
            privilegedExecutor.observeStatus().collect {
                _uiState.update { state -> state.copy(tier = repository.activeTier()) }
            }
        }
        viewModelScope.launch {
            backupPreferences.observe().collect { prefs ->
                _uiState.update {
                    it.copy(
                        prefs = prefs,
                        treeLabel = treeStore.displayLabel(prefs.treeUri),
                    )
                }
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

    fun setEncryptEnabled(enabled: Boolean) = _uiState.update { it.copy(encryptEnabled = enabled) }

    fun setPasswordDraft(value: String) = _uiState.update { it.copy(passwordDraft = value) }

    fun setKeepN(keepN: Int) {
        viewModelScope.launch {
            backupPreferences.setKeepN(BackupPrefs.clampKeepN(keepN))
        }
    }

    fun onTreePicked(uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch {
            treeStore.takePersistablePermission(uri, contentResolver)
            backupPreferences.setTreeUri(uri.toString())
        }
    }

    fun clearTree(contentResolver: ContentResolver) {
        viewModelScope.launch {
            val current = backupPreferences.current().treeUri
            treeStore.releasePersistablePermission(current, contentResolver)
            backupPreferences.setTreeUri(null)
        }
    }

    /** @return true if the UI should launch CreateDocument (no SAF tree). */
    fun requestBackup(): Boolean {
        val pkg = _uiState.value.selectedPackage ?: return false
        if (_uiState.value.encryptEnabled) {
            _uiState.update {
                it.copy(pendingBackupPackage = pkg, passwordPrompt = BackupPasswordPrompt.BACKUP, passwordDraft = "")
            }
            return false
        }
        return beginBackup(pkg, password = null)
    }

    fun confirmPasswordPrompt(contentResolver: ContentResolver): Boolean {
        val state = _uiState.value
        val password = state.passwordDraft.toCharArray()
        return when (state.passwordPrompt) {
            BackupPasswordPrompt.BACKUP -> {
                val pkg = state.pendingBackupPackage ?: return false
                _uiState.update { it.copy(passwordPrompt = null, passwordDraft = "") }
                beginBackup(pkg, password)
            }

            BackupPasswordPrompt.RESTORE -> {
                val uri = pendingRestoreUri ?: return false
                _uiState.update { it.copy(passwordPrompt = null, passwordDraft = "") }
                performRestore(uri, contentResolver, password)
                false
            }

            null -> false
        }
    }

    fun dismissPasswordPrompt() {
        pendingRestoreUri = null
        pendingPassword = null
        _uiState.update {
            it.copy(
                passwordPrompt = null,
                passwordDraft = "",
                pendingBackupPackage = null,
            )
        }
    }

    fun dismissBackup() {
        pendingPassword = null
        _uiState.update { it.copy(pendingBackupPackage = null) }
    }

    fun requestRestore() = _uiState.update { it.copy(pendingRestore = true) }

    fun dismissRestore() = _uiState.update { it.copy(pendingRestore = false) }

    fun suggestedFileName(packageName: String): String {
        val app = _uiState.value.apps.find { it.packageName == packageName }
        return repository.suggestedFileName(packageName, app?.versionName)
    }

    fun performBackup(uri: Uri, contentResolver: ContentResolver) {
        val pkg = _uiState.value.pendingBackupPackage ?: return
        val password = pendingPassword
        pendingPassword = null
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true, pendingBackupPackage = null, message = null) }
            when (val result = repository.backupToUri(pkg, uri, contentResolver, password)) {
                is Result.Failure -> _uiState.update { it.copy(busy = false, message = result.message) }

                is Result.Success -> _uiState.update {
                    it.copy(busy = false, message = "backup_ok:${result.value.packageName}")
                }
            }
        }
    }

    fun onRestoreFilePicked(uri: Uri, contentResolver: ContentResolver) {
        performRestore(uri, contentResolver, password = null)
    }

    fun performRestore(uri: Uri, contentResolver: ContentResolver, password: CharArray?) {
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true, pendingRestore = false, message = null) }
            when (val result = repository.restoreFromUri(uri, contentResolver, password)) {
                is Result.Failure -> {
                    if (result.message == "password_required") {
                        pendingRestoreUri = uri
                        _uiState.update {
                            it.copy(
                                busy = false,
                                passwordPrompt = BackupPasswordPrompt.RESTORE,
                                passwordDraft = "",
                            )
                        }
                    } else {
                        _uiState.update { it.copy(busy = false, message = result.message) }
                    }
                }

                is Result.Success -> {
                    pendingRestoreUri = null
                    _uiState.update { it.copy(busy = false, message = "restore_queued:${result.value}") }
                }
            }
        }
    }

    fun consumeMessage() = _uiState.update { it.copy(message = null) }

    /**
     * Starts backup to the SAF tree when set; otherwise arms [pendingBackupPackage] for CreateDocument.
     * @return true if CreateDocument should be launched.
     */
    private fun beginBackup(packageName: String, password: CharArray?): Boolean {
        if (_uiState.value.hasTree) {
            viewModelScope.launch {
                _uiState.update { it.copy(busy = true, pendingBackupPackage = null, message = null) }
                when (val result = repository.backupToTree(packageName, password)) {
                    is Result.Failure -> _uiState.update { it.copy(busy = false, message = result.message) }

                    is Result.Success -> _uiState.update {
                        it.copy(busy = false, message = "backup_ok:${result.value.packageName}")
                    }
                }
            }
            return false
        }
        pendingPassword = password
        _uiState.update { it.copy(pendingBackupPackage = packageName) }
        return true
    }
}

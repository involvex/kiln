package dev.involvex.kiln.feature.backup

import android.content.ContentResolver
import android.net.Uri
import android.os.Process
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.core.model.AndroidUserInfo
import dev.involvex.kiln.core.model.BackupPreferences
import dev.involvex.kiln.core.model.BackupPrefs
import dev.involvex.kiln.core.model.DualUserInstallGate
import dev.involvex.kiln.core.model.PrivilegeCapability
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
@Suppress("TooManyFunctions") // Backup hub: tree, encrypt, restore, dual-user confirm, SAF create.
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

    /** Remembered across SAF dismiss → file pick (M58). Null = current-user default. */
    private var rememberedTargetUserId: Int? = null

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

    fun requestBackup() {
        val pkg = _uiState.value.selectedPackage ?: return
        if (_uiState.value.encryptEnabled) {
            _uiState.update {
                it.copy(pendingBackupPackage = pkg, passwordPrompt = BackupPasswordPrompt.BACKUP, passwordDraft = "")
            }
            return
        }
        beginBackupOrUserConfirm(pkg, password = null)
    }

    fun confirmPasswordPrompt(contentResolver: ContentResolver) {
        val state = _uiState.value
        val password = state.passwordDraft.toCharArray()
        when (state.passwordPrompt) {
            BackupPasswordPrompt.BACKUP -> {
                val pkg = state.pendingBackupPackage ?: return
                _uiState.update { it.copy(passwordPrompt = null, passwordDraft = "") }
                beginBackupOrUserConfirm(pkg, password)
            }

            BackupPasswordPrompt.RESTORE -> {
                val uri = pendingRestoreUri ?: return
                _uiState.update { it.copy(passwordPrompt = null, passwordDraft = "") }
                performRestore(uri, contentResolver, password)
            }

            null -> Unit
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
                pendingBackupUserConfirm = false,
                confirmUsers = emptyList(),
            )
        }
    }

    fun dismissBackup() {
        pendingPassword = null
        rememberedTargetUserId = null
        _uiState.update {
            it.copy(
                pendingBackupPackage = null,
                pendingBackupUserConfirm = false,
                pendingCreateDocumentName = null,
                confirmUsers = emptyList(),
            )
        }
    }

    fun consumeCreateDocument() = _uiState.update { it.copy(pendingCreateDocumentName = null) }

    fun requestRestore() {
        viewModelScope.launch {
            val users = loadCensusUsersForPicker()
            rememberedTargetUserId = null
            _uiState.update {
                it.copy(
                    pendingRestore = true,
                    confirmUsers = users,
                    selectedUserId = currentUserId(),
                )
            }
        }
    }

    fun dismissRestore() {
        rememberedTargetUserId = null
        _uiState.update {
            it.copy(pendingRestore = false, confirmUsers = emptyList())
        }
    }

    fun selectConfirmUser(userId: Int) {
        _uiState.update { state ->
            if (!state.pendingRestore && !state.pendingBackupUserConfirm) return@update state
            state.copy(
                selectedUserId = DualUserInstallGate.resolveTargetUserId(
                    selectedUserId = userId,
                    users = state.confirmUsers,
                    currentUserId = currentUserId(),
                ),
            )
        }
    }

    /** Confirm restore dialog: remember user, then UI opens the SAF picker. */
    fun confirmRestore() {
        val state = _uiState.value
        rememberedTargetUserId = if (state.confirmUsers.isEmpty()) {
            null
        } else {
            DualUserInstallGate.resolveTargetUserId(
                selectedUserId = state.selectedUserId,
                users = state.confirmUsers,
                currentUserId = currentUserId(),
            )
        }
        _uiState.update {
            it.copy(pendingRestore = false, confirmUsers = emptyList())
        }
    }

    fun confirmBackupUser() {
        val state = _uiState.value
        if (!state.pendingBackupUserConfirm) return
        val pkg = state.pendingBackupPackage ?: return
        val password = pendingPassword
        rememberedTargetUserId = DualUserInstallGate.resolveTargetUserId(
            selectedUserId = state.selectedUserId,
            users = state.confirmUsers,
            currentUserId = currentUserId(),
        )
        _uiState.update {
            it.copy(pendingBackupUserConfirm = false, confirmUsers = emptyList())
        }
        finishBeginBackup(pkg, password, rememberedTargetUserId)
    }

    fun dismissBackupUserConfirm() {
        pendingPassword = null
        rememberedTargetUserId = null
        _uiState.update {
            it.copy(
                pendingBackupUserConfirm = false,
                pendingBackupPackage = null,
                confirmUsers = emptyList(),
            )
        }
    }

    fun suggestedFileName(packageName: String): String {
        val app = _uiState.value.apps.find { it.packageName == packageName }
        return repository.suggestedFileName(packageName, app?.versionName)
    }

    fun performBackup(uri: Uri, contentResolver: ContentResolver) {
        val pkg = _uiState.value.pendingBackupPackage ?: return
        val password = pendingPassword
        pendingPassword = null
        val targetUserId = rememberedTargetUserId
        val current = currentUserId()
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    busy = true,
                    pendingBackupPackage = null,
                    pendingBackupUserConfirm = false,
                    pendingCreateDocumentName = null,
                    confirmUsers = emptyList(),
                    message = null,
                )
            }
            when (
                val result = repository.backupToUri(
                    packageName = pkg,
                    targetUri = uri,
                    contentResolver = contentResolver,
                    password = password,
                    targetUserId = targetUserId,
                    currentUserId = current,
                )
            ) {
                is Result.Failure -> _uiState.update { it.copy(busy = false, message = result.message) }

                is Result.Success -> {
                    rememberedTargetUserId = null
                    _uiState.update {
                        it.copy(busy = false, message = "backup_ok:${result.value.packageName}")
                    }
                }
            }
        }
    }

    fun onRestoreFilePicked(uri: Uri, contentResolver: ContentResolver) {
        performRestore(uri, contentResolver, password = null)
    }

    fun performRestore(uri: Uri, contentResolver: ContentResolver, password: CharArray?) {
        val targetUserId = rememberedTargetUserId
        val current = currentUserId()
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true, pendingRestore = false, message = null) }
            when (
                val result = repository.restoreFromUri(
                    uri = uri,
                    contentResolver = contentResolver,
                    password = password,
                    targetUserId = targetUserId,
                    currentUserId = current,
                )
            ) {
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
                        rememberedTargetUserId = null
                        _uiState.update { it.copy(busy = false, message = result.message) }
                    }
                }

                is Result.Success -> {
                    pendingRestoreUri = null
                    rememberedTargetUserId = null
                    _uiState.update {
                        it.copy(busy = false, message = "restore_queued:${result.value}")
                    }
                }
            }
        }
    }

    fun consumeMessage() = _uiState.update { it.copy(message = null) }

    private fun beginBackupOrUserConfirm(packageName: String, password: CharArray?) {
        viewModelScope.launch {
            val users = loadCensusUsersForPicker()
            if (users.isEmpty()) {
                rememberedTargetUserId = null
                finishBeginBackup(packageName, password, targetUserId = null)
            } else {
                pendingPassword = password
                _uiState.update {
                    it.copy(
                        pendingBackupPackage = packageName,
                        pendingBackupUserConfirm = true,
                        confirmUsers = users,
                        selectedUserId = currentUserId(),
                    )
                }
            }
        }
    }

    private fun finishBeginBackup(packageName: String, password: CharArray?, targetUserId: Int?) {
        val current = currentUserId()
        if (_uiState.value.hasTree) {
            viewModelScope.launch {
                _uiState.update {
                    it.copy(
                        busy = true,
                        pendingBackupPackage = null,
                        pendingBackupUserConfirm = false,
                        pendingCreateDocumentName = null,
                        confirmUsers = emptyList(),
                        message = null,
                    )
                }
                when (
                    val result = repository.backupToTree(
                        packageName = packageName,
                        password = password,
                        targetUserId = targetUserId,
                        currentUserId = current,
                    )
                ) {
                    is Result.Failure -> _uiState.update { it.copy(busy = false, message = result.message) }

                    is Result.Success -> {
                        rememberedTargetUserId = null
                        _uiState.update {
                            it.copy(busy = false, message = "backup_ok:${result.value.packageName}")
                        }
                    }
                }
            }
            return
        }
        pendingPassword = password
        _uiState.update {
            it.copy(
                pendingBackupPackage = packageName,
                pendingCreateDocumentName = suggestedFileName(packageName),
            )
        }
    }

    private suspend fun loadCensusUsersForPicker(): List<AndroidUserInfo> {
        val caps = privilegedExecutor.currentStatus().capabilities
        val hasSilent = PrivilegeCapability.SILENT_INSTALL in caps
        if (PrivilegeCapability.RUN_SHELL_DIAGNOSTIC !in caps) return emptyList()
        val users = when (val result = privilegedExecutor.listAndroidUsers()) {
            is Result.Success -> result.value
            is Result.Failure -> emptyList()
        }
        return if (DualUserInstallGate.showPicker(users, hasSilent)) users else emptyList()
    }

    private fun currentUserId(): Int = Process.myUid() / ANDROID_PER_USER_RANGE

    private companion object {
        const val ANDROID_PER_USER_RANGE = 100_000
    }
}

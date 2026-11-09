package dev.involvex.kiln.feature.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.core.model.ApkExportFormat
import dev.involvex.kiln.core.model.ApkRollbackCache
import dev.involvex.kiln.core.model.AppUpdateRepository
import dev.involvex.kiln.core.model.GitHubAuthHeaderProvider
import dev.involvex.kiln.core.model.GitHubReleaseChannel
import dev.involvex.kiln.core.model.InstallQueue
import dev.involvex.kiln.core.model.MatchConfidence
import dev.involvex.kiln.core.model.PrivilegeCapability
import dev.involvex.kiln.core.model.PrivilegedExecutor
import dev.involvex.kiln.core.model.RollbackApkEntry
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
    private val rollbackCache: ApkRollbackCache,
    private val privilegedExecutor: PrivilegedExecutor,
    private val gitHubAuth: GitHubAuthHeaderProvider,
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
        viewModelScope.launch {
            appUpdateRepository.observeIgnoreUpdates(packageName).collect { ignore ->
                _uiState.update { it.copy(ignoreUpdates = ignore) }
            }
        }
        viewModelScope.launch {
            appUpdateRepository.observeManagedLink(packageName).collect { link ->
                _uiState.update { it.copy(managedLink = link) }
            }
        }
        viewModelScope.launch {
            appUpdateRepository.observeEnabledRepos().collect { repos ->
                _uiState.update { it.copy(enabledRepos = repos) }
            }
        }
        viewModelScope.launch {
            rollbackCache.observe(packageName).collect { entries ->
                val installedCode = _uiState.value.detail?.versionCode
                val older = entries.filter { installedCode == null || it.versionCode < installedCode }
                _uiState.update { it.copy(rollbackEntries = older) }
            }
        }
        viewModelScope.launch {
            privilegedExecutor.observeStatus().collect { status ->
                _uiState.update {
                    it.copy(canSilentDowngrade = PrivilegeCapability.SILENT_INSTALL in status.capabilities)
                }
            }
        }
        viewModelScope.launch {
            _uiState.update { it.copy(hasGitHubToken = gitHubAuth.hasToken()) }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = repository.detail(packageName)) {
                is Result.Success -> {
                    val detail = result.value
                    val rollback = rollbackCache.list(packageName)
                        .filter { it.versionCode < detail.versionCode }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            detail = detail,
                            installerLabel = InstallerLabels.displayName(detail.installerPackage),
                            errorMessage = null,
                            rollbackEntries = rollback,
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

            is AppDetailEvent.FormatSelected -> applyExportFormat(event.format)

            AppDetailEvent.ClearSnackbar -> _uiState.update { it.copy(snackbarMessage = null) }

            AppDetailEvent.InstallUpdate -> enqueueUpdate()

            is AppDetailEvent.IgnoreUpdatesChanged -> setCheckForUpdates(event.checkForUpdates)

            is AppDetailEvent.RequestRollback -> requestRollback(event.entry)

            AppDetailEvent.DismissRollback -> _uiState.update { it.copy(pendingRollback = null) }

            AppDetailEvent.ConfirmRollback -> confirmRollback()

            AppDetailEvent.OpenLinkSource,
            AppDetailEvent.DismissLinkSource,
            is AppDetailEvent.LinkFdroidRepo,
            is AppDetailEvent.LinkGithub,
            is AppDetailEvent.SetGitHubChannel,
            -> onLinkEvent(event)

            AppDetailEvent.OpenUnknownSourcesSettings,
            AppDetailEvent.Launch,
            AppDetailEvent.OpenAppInfo,
            AppDetailEvent.Uninstall,
            -> Unit // handled in UI via intents
        }
    }

    private fun onLinkEvent(event: AppDetailEvent) {
        when (event) {
            AppDetailEvent.OpenLinkSource -> _uiState.update { it.copy(showLinkSourceDialog = true) }
            AppDetailEvent.DismissLinkSource -> _uiState.update { it.copy(showLinkSourceDialog = false) }
            is AppDetailEvent.LinkFdroidRepo -> linkFdroid(event.repoId)
            is AppDetailEvent.LinkGithub -> linkGithub(event.ownerRepo)
            is AppDetailEvent.SetGitHubChannel -> setGitHubChannel(event.channel)
            else -> Unit
        }
    }

    private fun setGitHubChannel(channel: GitHubReleaseChannel) {
        viewModelScope.launch {
            _uiState.update { it.copy(hasGitHubToken = gitHubAuth.hasToken()) }
            when (val result = appUpdateRepository.setGitHubChannel(packageName, channel)) {
                is Result.Success -> Unit
                is Result.Failure -> showMessage(result.message)
            }
        }
    }

    private fun linkFdroid(repoId: String) {
        viewModelScope.launch {
            when (
                val result = appUpdateRepository.linkManagedSource(
                    packageName = packageName,
                    sourceId = repoId,
                    confidence = MatchConfidence.MEDIUM,
                    pinned = true,
                )
            ) {
                is Result.Success -> {
                    _uiState.update { it.copy(showLinkSourceDialog = false) }
                    showMessage("link_ok")
                }

                is Result.Failure -> showMessage(result.message)
            }
        }
    }

    private fun linkGithub(ownerRepo: String) {
        val normalized = ownerRepo.trim().trim('/')
        if (!normalized.contains('/') || normalized.count { it == '/' } != 1) {
            showMessage("bad_github")
            return
        }
        viewModelScope.launch {
            when (
                val result = appUpdateRepository.linkManagedSource(
                    packageName = packageName,
                    sourceId = "github:$normalized",
                    confidence = MatchConfidence.MEDIUM,
                    githubOwnerRepo = normalized,
                    githubAssetRegex = ".*\\.apk$",
                    pinned = true,
                )
            ) {
                is Result.Success -> {
                    _uiState.update { it.copy(showLinkSourceDialog = false) }
                    showMessage("link_ok")
                }

                is Result.Failure -> showMessage(result.message)
            }
        }
    }

    private fun applyExportFormat(format: ApkExportFormat) {
        _uiState.update { state ->
            state.copy(exportDialog = state.exportDialog?.copy(format = format))
        }
    }

    private fun setCheckForUpdates(checkForUpdates: Boolean) {
        // Switch ON = check for updates = ignoreUpdates false
        viewModelScope.launch {
            when (
                val result = appUpdateRepository.setIgnoreUpdates(
                    packageName,
                    ignoreUpdates = !checkForUpdates,
                )
            ) {
                is Result.Success -> Unit
                is Result.Failure -> showMessage(result.message)
            }
        }
    }

    private fun requestRollback(entry: RollbackApkEntry) {
        if (!_uiState.value.canSilentDowngrade) {
            showMessage("rollback_needs_privilege")
            return
        }
        _uiState.update { it.copy(pendingRollback = entry) }
    }

    fun setExportInProgress(inProgress: Boolean) {
        _uiState.update { it.copy(exportInProgress = inProgress) }
    }

    fun showMessage(message: String) {
        _uiState.update { it.copy(snackbarMessage = message) }
    }

    private fun confirmRollback() {
        val entry = _uiState.value.pendingRollback ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(rollbackInProgress = true, pendingRollback = null) }
            when (val result = privilegedExecutor.installApk(entry.apkAbsolutePath, allowDowngrade = true)) {
                is Result.Success -> {
                    showMessage("rollback_ok")
                    refresh()
                }

                is Result.Failure -> showMessage(result.message)
            }
            _uiState.update { it.copy(rollbackInProgress = false) }
        }
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

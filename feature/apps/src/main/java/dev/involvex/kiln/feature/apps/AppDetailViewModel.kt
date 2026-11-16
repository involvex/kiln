package dev.involvex.kiln.feature.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.core.model.ApkExportFormat
import dev.involvex.kiln.core.model.ApkRollbackCache
import dev.involvex.kiln.core.model.AppUpdateRepository
import dev.involvex.kiln.core.model.BackgroundUpdatePreferences
import dev.involvex.kiln.core.model.GitHubAuthHeaderProvider
import dev.involvex.kiln.core.model.GitHubReleaseChannel
import dev.involvex.kiln.core.model.InstallQueue
import dev.involvex.kiln.core.model.MatchConfidence
import dev.involvex.kiln.core.model.PrivilegeCapability
import dev.involvex.kiln.core.model.PrivilegedExecutor
import dev.involvex.kiln.core.model.RollbackApkEntry
import dev.involvex.kiln.feature.apps.activities.AppActivitiesRepository
import dev.involvex.kiln.feature.apps.data.InstalledAppsRepository
import dev.involvex.kiln.feature.apps.data.InstallerLabels
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
@Suppress("TooManyFunctions") // Detail hub: updates, link, rollback, activities, export.
class AppDetailViewModel @Inject constructor(
    private val repository: InstalledAppsRepository,
    private val activitiesRepository: AppActivitiesRepository,
    private val appUpdateRepository: AppUpdateRepository,
    private val installQueue: InstallQueue,
    private val rollbackCache: ApkRollbackCache,
    private val privilegedExecutor: PrivilegedExecutor,
    private val gitHubAuth: GitHubAuthHeaderProvider,
    private val backgroundUpdatePreferences: BackgroundUpdatePreferences,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AppDetailUiState())
    val uiState: StateFlow<AppDetailUiState> = _uiState.asStateFlow()

    private var packageName: String? = null
    private var packageJobs: List<Job> = emptyList()

    fun load(packageName: String) {
        if (this.packageName == packageName && _uiState.value.detail?.packageName == packageName) {
            return
        }
        this.packageName = packageName
        packageJobs.forEach { it.cancel() }
        _uiState.value = AppDetailUiState(isLoading = true)
        packageJobs = listOf(
            viewModelScope.launch {
                appUpdateRepository.updates().collect { map ->
                    _uiState.update { it.copy(availableUpdate = map[packageName]) }
                }
            },
            viewModelScope.launch {
                installQueue.jobs().collect { jobs ->
                    _uiState.update { it.copy(installJob = jobs[packageName]) }
                }
            },
            viewModelScope.launch {
                appUpdateRepository.observeIgnoreUpdates(packageName).collect { ignore ->
                    _uiState.update { it.copy(ignoreUpdates = ignore) }
                }
            },
            viewModelScope.launch {
                appUpdateRepository.observeUpdateCooldownDays(packageName).collect { days ->
                    _uiState.update { it.copy(updateCooldownDays = days) }
                }
            },
            viewModelScope.launch {
                backgroundUpdatePreferences.observe().collect { prefs ->
                    _uiState.update { it.copy(globalCooldownDays = prefs.updateCooldownDays) }
                }
            },
            viewModelScope.launch {
                appUpdateRepository.observeManagedLink(packageName).collect { link ->
                    _uiState.update { it.copy(managedLink = link) }
                }
            },
            viewModelScope.launch {
                appUpdateRepository.observeEnabledRepos().collect { repos ->
                    _uiState.update { it.copy(enabledRepos = repos) }
                }
            },
            viewModelScope.launch {
                rollbackCache.observe(packageName).collect { entries ->
                    val installedCode = _uiState.value.detail?.versionCode
                    val older = entries.filter { installedCode == null || it.versionCode < installedCode }
                    _uiState.update { it.copy(rollbackEntries = older) }
                }
            },
            viewModelScope.launch {
                privilegedExecutor.observeStatus().collect { status ->
                    _uiState.update {
                        it.copy(canSilentDowngrade = PrivilegeCapability.SILENT_INSTALL in status.capabilities)
                    }
                }
            },
            viewModelScope.launch {
                _uiState.update { it.copy(hasGitHubToken = gitHubAuth.hasToken()) }
            },
        )
        refresh()
        loadActivities()
    }

    fun setActivitiesQuery(query: String) {
        _uiState.update { it.copy(activitiesQuery = query) }
    }

    fun refresh() {
        val pkg = packageName ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = repository.detail(pkg)) {
                is Result.Success -> {
                    val detail = result.value
                    val rollback = rollbackCache.list(pkg)
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
        loadActivities()
    }

    fun onEvent(event: AppDetailEvent) {
        when (event) {
            AppDetailEvent.Refresh -> refresh()

            AppDetailEvent.Export -> openExport()

            AppDetailEvent.DismissExport -> _uiState.update { it.copy(exportDialog = null) }

            is AppDetailEvent.FormatSelected -> applyExportFormat(event.format)

            AppDetailEvent.ClearSnackbar -> _uiState.update { it.copy(snackbarMessage = null) }

            AppDetailEvent.InstallUpdate,
            AppDetailEvent.RequestInstallDespiteSignature,
            AppDetailEvent.DismissInstallDespiteSignature,
            AppDetailEvent.ConfirmInstallDespiteSignature,
            -> onInstallEvent(event)

            is AppDetailEvent.IgnoreUpdatesChanged -> setCheckForUpdates(event.checkForUpdates)

            is AppDetailEvent.UpdateCooldownChanged -> setUpdateCooldownDays(event.days)

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

    private fun loadActivities() {
        val pkg = packageName ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(activitiesLoading = true) }
            val list = activitiesRepository.exportedActivities(pkg)
            _uiState.update { it.copy(activities = list, activitiesLoading = false) }
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
        val pkg = packageName ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(hasGitHubToken = gitHubAuth.hasToken()) }
            when (val result = appUpdateRepository.setGitHubChannel(pkg, channel)) {
                is Result.Success -> Unit
                is Result.Failure -> showMessage(result.message)
            }
        }
    }

    private fun linkFdroid(repoId: String) {
        val pkg = packageName ?: return
        viewModelScope.launch {
            when (
                val result = appUpdateRepository.linkManagedSource(
                    packageName = pkg,
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
        val pkg = packageName ?: return
        val normalized = ownerRepo.trim().trim('/')
        if (!normalized.contains('/') || normalized.count { it == '/' } != 1) {
            showMessage("bad_github")
            return
        }
        viewModelScope.launch {
            when (
                val result = appUpdateRepository.linkManagedSource(
                    packageName = pkg,
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
        val pkg = packageName ?: return
        viewModelScope.launch {
            when (
                val result = appUpdateRepository.setIgnoreUpdates(
                    pkg,
                    ignoreUpdates = !checkForUpdates,
                )
            ) {
                is Result.Success -> Unit
                is Result.Failure -> showMessage(result.message)
            }
        }
    }

    private fun setUpdateCooldownDays(days: Int?) {
        val pkg = packageName ?: return
        viewModelScope.launch {
            when (val result = appUpdateRepository.setUpdateCooldownDays(pkg, days)) {
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

    private fun onInstallEvent(event: AppDetailEvent) {
        when (event) {
            AppDetailEvent.InstallUpdate -> enqueueUpdate(allowSignatureMismatch = false)

            AppDetailEvent.RequestInstallDespiteSignature ->
                _uiState.update { it.copy(confirmSignatureMismatchInstall = true) }

            AppDetailEvent.DismissInstallDespiteSignature ->
                _uiState.update { it.copy(confirmSignatureMismatchInstall = false) }

            AppDetailEvent.ConfirmInstallDespiteSignature -> {
                _uiState.update { it.copy(confirmSignatureMismatchInstall = false) }
                enqueueUpdate(allowSignatureMismatch = true)
            }

            else -> Unit
        }
    }

    private fun enqueueUpdate(allowSignatureMismatch: Boolean) {
        val update = _uiState.value.availableUpdate ?: return
        viewModelScope.launch {
            when (val result = installQueue.enqueue(update, allowSignatureMismatch = allowSignatureMismatch)) {
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
}

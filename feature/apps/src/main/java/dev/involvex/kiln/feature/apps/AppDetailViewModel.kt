package dev.involvex.kiln.feature.apps

import android.os.Process
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.core.model.ActivityLogEvents
import dev.involvex.kiln.core.model.ActivityLogRecorder
import dev.involvex.kiln.core.model.ActivityLogType
import dev.involvex.kiln.core.model.AndroidUserInfo
import dev.involvex.kiln.core.model.ApkExportFormat
import dev.involvex.kiln.core.model.ApkRollbackCache
import dev.involvex.kiln.core.model.AppComponentInfo
import dev.involvex.kiln.core.model.AppUpdateRepository
import dev.involvex.kiln.core.model.BackgroundUpdatePreferences
import dev.involvex.kiln.core.model.ComponentTogglePolicy
import dev.involvex.kiln.core.model.DualUserInstallGate
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
import dev.involvex.kiln.feature.apps.denylist.AppProtectedPackageResolver
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
@Suppress("TooManyFunctions", "LargeClass") // Detail hub: updates, link, rollback, activities, dual-user, export.
class AppDetailViewModel @Inject constructor(
    private val repository: InstalledAppsRepository,
    private val activitiesRepository: AppActivitiesRepository,
    private val appUpdateRepository: AppUpdateRepository,
    private val installQueue: InstallQueue,
    private val rollbackCache: ApkRollbackCache,
    private val privilegedExecutor: PrivilegedExecutor,
    private val gitHubAuth: GitHubAuthHeaderProvider,
    private val backgroundUpdatePreferences: BackgroundUpdatePreferences,
    private val protectedPackageResolver: AppProtectedPackageResolver,
    private val activityLog: ActivityLogRecorder,
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
                appUpdateRepository.observeSavedGithubOwnerRepos().collect { repos ->
                    _uiState.update { it.copy(savedGithubRepos = repos) }
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
                    val deny = protectedPackageResolver.resolve()
                    val pkg = packageName
                    _uiState.update {
                        it.copy(
                            canSilentDowngrade = PrivilegeCapability.SILENT_INSTALL in status.capabilities,
                            canToggleComponents = PrivilegeCapability.DEBLOAT in status.capabilities,
                            componentPackageDenylisted = pkg != null &&
                                ComponentTogglePolicy.isDenylisted(pkg, deny.keys),
                        )
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

            is AppDetailEvent.RequestRollback,
            AppDetailEvent.DismissRollback,
            AppDetailEvent.ConfirmRollback,
            is AppDetailEvent.RequestComponentToggle,
            is AppDetailEvent.SelectComponentToggleUser,
            AppDetailEvent.DismissComponentToggle,
            AppDetailEvent.ConfirmComponentToggle,
            -> onConfirmableEvent(event)

            AppDetailEvent.OpenLinkSource,
            AppDetailEvent.DismissLinkSource,
            is AppDetailEvent.LinkFdroidRepo,
            is AppDetailEvent.LinkGithub,
            is AppDetailEvent.SetGitHubChannel,
            AppDetailEvent.RequestStopTracking,
            AppDetailEvent.ConfirmStopTracking,
            AppDetailEvent.DismissStopTracking,
            -> onLinkEvent(event)

            AppDetailEvent.Uninstall,
            AppDetailEvent.ClearOpenSystemUninstall,
            is AppDetailEvent.SelectDualUser,
            AppDetailEvent.ConfirmDualUserAction,
            AppDetailEvent.DismissDualUserAction,
            -> onDualUserEvent(event)

            AppDetailEvent.OpenUnknownSourcesSettings,
            AppDetailEvent.Launch,
            AppDetailEvent.OpenAppInfo,
            -> Unit // handled in UI via intents
        }
    }

    private fun onDualUserEvent(event: AppDetailEvent) {
        when (event) {
            AppDetailEvent.Uninstall -> requestUninstall()

            AppDetailEvent.ClearOpenSystemUninstall ->
                _uiState.update { it.copy(openSystemUninstall = false) }

            is AppDetailEvent.SelectDualUser ->
                _uiState.update { state ->
                    val pending = state.pendingDualUserAction ?: return@update state
                    state.copy(
                        pendingDualUserAction = pending.copy(
                            selectedUserId = DualUserInstallGate.resolveTargetUserId(
                                selectedUserId = event.userId,
                                users = pending.users,
                                currentUserId = currentUserId(),
                            ),
                        ),
                    )
                }

            AppDetailEvent.ConfirmDualUserAction -> confirmDualUserAction()

            AppDetailEvent.DismissDualUserAction ->
                _uiState.update { it.copy(pendingDualUserAction = null) }

            else -> Unit
        }
    }

    /** Android multi-user: userId = uid / [ANDROID_PER_USER_RANGE]. */
    private fun currentUserId(): Int = Process.myUid() / ANDROID_PER_USER_RANGE

    private fun requestUninstall() {
        viewModelScope.launch {
            val caps = privilegedExecutor.currentStatus().capabilities
            val hasSilent = PrivilegeCapability.SILENT_UNINSTALL in caps
            val users = loadCensusUsers()
            if (DualUserInstallGate.showPicker(users, hasSilent)) {
                _uiState.update {
                    it.copy(
                        pendingDualUserAction = PendingDualUserAction(
                            kind = DualUserPendingKind.Uninstall,
                            users = users,
                            selectedUserId = currentUserId(),
                        ),
                    )
                }
            } else {
                _uiState.update { it.copy(openSystemUninstall = true) }
            }
        }
    }

    private suspend fun loadCensusUsers(): List<AndroidUserInfo> {
        val caps = privilegedExecutor.currentStatus().capabilities
        if (PrivilegeCapability.RUN_SHELL_DIAGNOSTIC !in caps) return emptyList()
        return when (val result = privilegedExecutor.listAndroidUsers()) {
            is Result.Success -> result.value
            is Result.Failure -> emptyList()
        }
    }

    private fun maybePromptDualUser(kind: DualUserPendingKind, silentCap: PrivilegeCapability, onSkip: () -> Unit) {
        viewModelScope.launch {
            val caps = privilegedExecutor.currentStatus().capabilities
            val hasSilent = silentCap in caps
            val users = loadCensusUsers()
            if (DualUserInstallGate.showPicker(users, hasSilent)) {
                _uiState.update {
                    it.copy(
                        pendingDualUserAction = PendingDualUserAction(
                            kind = kind,
                            users = users,
                            selectedUserId = currentUserId(),
                        ),
                    )
                }
            } else {
                onSkip()
            }
        }
    }

    private fun confirmDualUserAction() {
        val pending = _uiState.value.pendingDualUserAction ?: return
        val userId = DualUserInstallGate.resolveTargetUserId(
            selectedUserId = pending.selectedUserId,
            users = pending.users,
            currentUserId = currentUserId(),
        )
        _uiState.update { it.copy(pendingDualUserAction = null) }
        when (pending.kind) {
            DualUserPendingKind.Uninstall -> performSilentUninstall(userId)
            DualUserPendingKind.InstallUpdate -> enqueueUpdate(allowSignatureMismatch = false, targetUserId = userId)
            DualUserPendingKind.InstallMismatch -> enqueueUpdate(allowSignatureMismatch = true, targetUserId = userId)
            DualUserPendingKind.Rollback -> confirmRollback(targetUserId = userId)
        }
    }

    private fun performSilentUninstall(targetUserId: Int) {
        val pkg = packageName ?: return
        viewModelScope.launch {
            when (val result = privilegedExecutor.uninstall(pkg, keepData = false, targetUserId = targetUserId)) {
                is Result.Success -> {
                    activityLog.record(
                        ActivityLogEvents.create(
                            type = ActivityLogType.INSTALL,
                            action = "uninstall",
                            success = true,
                            packageName = pkg,
                            detailKey = if (targetUserId != currentUserId()) "user:$targetUserId" else null,
                        ),
                    )
                    showMessage("uninstall_ok")
                    refresh()
                }

                is Result.Failure -> showMessage(result.message)
            }
        }
    }

    private fun loadActivities() {
        val pkg = packageName ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(activitiesLoading = true) }
            val components = activitiesRepository.exportedComponents(pkg)
            val deny = protectedPackageResolver.resolve()
            val caps = privilegedExecutor.currentStatus().capabilities
            _uiState.update {
                it.copy(
                    activities = components.activities,
                    services = components.services,
                    receivers = components.receivers,
                    providers = components.providers,
                    activitiesLoading = false,
                    canToggleComponents = PrivilegeCapability.DEBLOAT in caps,
                    componentPackageDenylisted = ComponentTogglePolicy.isDenylisted(pkg, deny.keys),
                )
            }
        }
    }

    private fun onConfirmableEvent(event: AppDetailEvent) {
        when (event) {
            is AppDetailEvent.RequestRollback -> requestRollback(event.entry)

            AppDetailEvent.DismissRollback -> _uiState.update { it.copy(pendingRollback = null) }

            AppDetailEvent.ConfirmRollback ->
                maybePromptDualUser(
                    kind = DualUserPendingKind.Rollback,
                    silentCap = PrivilegeCapability.SILENT_INSTALL,
                    onSkip = { confirmRollback(targetUserId = 0) },
                )

            is AppDetailEvent.RequestComponentToggle -> requestComponentToggle(event.component, event.enable)

            is AppDetailEvent.SelectComponentToggleUser ->
                _uiState.update { state ->
                    val pending = state.pendingComponentToggle ?: return@update state
                    state.copy(
                        pendingComponentToggle = pending.copy(
                            selectedUserId = DualUserInstallGate.resolveTargetUserId(
                                selectedUserId = event.userId,
                                users = pending.users,
                                currentUserId = currentUserId(),
                            ),
                        ),
                    )
                }

            AppDetailEvent.DismissComponentToggle -> _uiState.update { it.copy(pendingComponentToggle = null) }

            AppDetailEvent.ConfirmComponentToggle -> confirmComponentToggle()

            else -> Unit
        }
    }

    private fun requestComponentToggle(component: AppComponentInfo, enable: Boolean) {
        val deny = protectedPackageResolver.resolve()
        if (!ComponentTogglePolicy.canOfferToggle(
                exported = component.exported,
                hasDebloatCapability = _uiState.value.canToggleComponents,
                packageName = component.packageName,
                denylistedPackages = deny.keys,
            )
        ) {
            showMessage(
                when {
                    !_uiState.value.canToggleComponents -> "component_needs_privilege"
                    ComponentTogglePolicy.isDenylisted(component.packageName, deny.keys) -> "component_denylisted"
                    !component.exported -> "component_not_exported"
                    else -> "component_toggle_blocked"
                },
            )
            return
        }
        viewModelScope.launch {
            val caps = privilegedExecutor.currentStatus().capabilities
            val hasDebloat = PrivilegeCapability.DEBLOAT in caps
            val users = loadCensusUsers()
            val showUserPicker = DualUserInstallGate.showPicker(users, hasDebloat)
            _uiState.update {
                it.copy(
                    pendingComponentToggle = PendingComponentToggle(
                        component = component,
                        enable = enable,
                        users = if (showUserPicker) users else emptyList(),
                        selectedUserId = currentUserId(),
                    ),
                )
            }
        }
    }

    private fun confirmComponentToggle() {
        val pending = _uiState.value.pendingComponentToggle ?: return
        val deny = protectedPackageResolver.resolve()
        if (ComponentTogglePolicy.isDenylisted(pending.component.packageName, deny.keys)) {
            _uiState.update { it.copy(pendingComponentToggle = null) }
            showMessage("component_denylisted")
            return
        }
        val targetUserId = DualUserInstallGate.resolveTargetUserId(
            selectedUserId = pending.selectedUserId,
            users = pending.users,
            currentUserId = currentUserId(),
        )
        viewModelScope.launch {
            _uiState.update { it.copy(componentToggleInProgress = true, pendingComponentToggle = null) }
            val result = if (pending.enable) {
                privilegedExecutor.enableComponent(
                    pending.component.packageName,
                    pending.component.className,
                    targetUserId = targetUserId,
                )
            } else {
                privilegedExecutor.disableComponent(
                    pending.component.packageName,
                    pending.component.className,
                    targetUserId = targetUserId,
                )
            }
            val ok = result is Result.Success
            activityLog.record(
                ActivityLogEvents.create(
                    type = ActivityLogType.SHELL,
                    action = if (pending.enable) "component_enable" else "component_disable",
                    success = ok,
                    packageName = pending.component.packageName,
                    detailKey = buildString {
                        append(pending.component.className.substringAfterLast('.').take(COMPONENT_DETAIL_MAX))
                        if (targetUserId != currentUserId()) append(";user:$targetUserId")
                    },
                ),
            )
            _uiState.update { it.copy(componentToggleInProgress = false) }
            when (result) {
                is Result.Success -> {
                    showMessage(if (pending.enable) "component_enabled" else "component_disabled")
                    loadActivities()
                }

                is Result.Failure -> showMessage(result.message)
            }
        }
    }

    private fun onLinkEvent(event: AppDetailEvent) {
        when (event) {
            AppDetailEvent.OpenLinkSource -> _uiState.update { it.copy(showLinkSourceDialog = true) }

            AppDetailEvent.DismissLinkSource -> _uiState.update { it.copy(showLinkSourceDialog = false) }

            is AppDetailEvent.LinkFdroidRepo -> linkFdroid(event.repoId)

            is AppDetailEvent.LinkGithub -> linkGithub(event.ownerRepo)

            is AppDetailEvent.SetGitHubChannel -> setGitHubChannel(event.channel)

            AppDetailEvent.RequestStopTracking ->
                _uiState.update { it.copy(showStopTrackingConfirm = true) }

            AppDetailEvent.DismissStopTracking ->
                _uiState.update { it.copy(showStopTrackingConfirm = false) }

            AppDetailEvent.ConfirmStopTracking -> confirmStopTracking()

            else -> Unit
        }
    }

    private fun confirmStopTracking() {
        val pkg = packageName ?: return
        viewModelScope.launch {
            when (val result = appUpdateRepository.unlinkManagedPackage(pkg)) {
                is Result.Success -> {
                    _uiState.update { it.copy(showStopTrackingConfirm = false) }
                    showMessage("unlinked")
                }

                is Result.Failure -> {
                    _uiState.update { it.copy(showStopTrackingConfirm = false) }
                    showMessage(result.message.ifBlank { "unlink_failed" })
                }
            }
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

    private fun confirmRollback(targetUserId: Int) {
        val entry = _uiState.value.pendingRollback ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(rollbackInProgress = true, pendingRollback = null) }
            when (
                val result = privilegedExecutor.installApk(
                    apkAbsolutePath = entry.apkAbsolutePath,
                    allowDowngrade = true,
                    targetUserId = targetUserId,
                )
            ) {
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
            AppDetailEvent.InstallUpdate ->
                maybePromptDualUser(
                    kind = DualUserPendingKind.InstallUpdate,
                    silentCap = PrivilegeCapability.SILENT_INSTALL,
                    onSkip = { enqueueUpdate(allowSignatureMismatch = false, targetUserId = null) },
                )

            AppDetailEvent.RequestInstallDespiteSignature ->
                _uiState.update { it.copy(confirmSignatureMismatchInstall = true) }

            AppDetailEvent.DismissInstallDespiteSignature ->
                _uiState.update { it.copy(confirmSignatureMismatchInstall = false) }

            AppDetailEvent.ConfirmInstallDespiteSignature -> {
                _uiState.update { it.copy(confirmSignatureMismatchInstall = false) }
                maybePromptDualUser(
                    kind = DualUserPendingKind.InstallMismatch,
                    silentCap = PrivilegeCapability.SILENT_INSTALL,
                    onSkip = { enqueueUpdate(allowSignatureMismatch = true, targetUserId = null) },
                )
            }

            else -> Unit
        }
    }

    private fun enqueueUpdate(allowSignatureMismatch: Boolean, targetUserId: Int?) {
        val update = _uiState.value.availableUpdate ?: return
        viewModelScope.launch {
            when (
                val result = installQueue.enqueue(
                    update = update,
                    allowSignatureMismatch = allowSignatureMismatch,
                    targetUserId = targetUserId,
                )
            ) {
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

    companion object {
        private const val COMPONENT_DETAIL_MAX = 64

        /** UID range per Android user (`Process.myUid() / this` → user id). */
        private const val ANDROID_PER_USER_RANGE = 100_000
    }
}

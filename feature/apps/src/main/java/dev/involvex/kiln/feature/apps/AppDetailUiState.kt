package dev.involvex.kiln.feature.apps

import dev.involvex.kiln.core.model.AndroidUserInfo
import dev.involvex.kiln.core.model.ApkExportFormat
import dev.involvex.kiln.core.model.AppActivityInfo
import dev.involvex.kiln.core.model.AppComponentFilter
import dev.involvex.kiln.core.model.AppComponentInfo
import dev.involvex.kiln.core.model.AppUpdateInfo
import dev.involvex.kiln.core.model.GitHubReleaseChannel
import dev.involvex.kiln.core.model.InstallJobState
import dev.involvex.kiln.core.model.InstalledAppDetail
import dev.involvex.kiln.core.model.ManagedSourceLink
import dev.involvex.kiln.core.model.RepoConfig
import dev.involvex.kiln.core.model.RollbackApkEntry
import dev.involvex.kiln.feature.apps.activities.AppActivityInfoQuery
import dev.involvex.kiln.feature.apps.activities.AppActivityLabels

data class PendingComponentToggle(val component: AppComponentInfo, val enable: Boolean)

enum class DualUserPendingKind {
    Uninstall,
    InstallUpdate,
    InstallMismatch,
    Rollback,
}

data class PendingDualUserAction(val kind: DualUserPendingKind, val users: List<AndroidUserInfo>, val selectedUserId: Int)

data class AppDetailUiState(
    val isLoading: Boolean = true,
    val detail: InstalledAppDetail? = null,
    val installerLabel: String? = null,
    val errorMessage: String? = null,
    val exportDialog: ExportDialogState? = null,
    val exportInProgress: Boolean = false,
    val snackbarMessage: String? = null,
    val availableUpdate: AppUpdateInfo? = null,
    val installJob: InstallJobState? = null,
    /** null = not managed yet; true = skipping update checks; false = checking updates */
    val ignoreUpdates: Boolean? = null,
    /** null = use [globalCooldownDays]; otherwise per-app override. */
    val updateCooldownDays: Int? = null,
    val globalCooldownDays: Int = 0,
    val managedLink: ManagedSourceLink? = null,
    val enabledRepos: List<RepoConfig> = emptyList(),
    val savedGithubRepos: List<String> = emptyList(),
    val showLinkSourceDialog: Boolean = false,
    val showStopTrackingConfirm: Boolean = false,
    val hasGitHubToken: Boolean = false,
    val rollbackEntries: List<RollbackApkEntry> = emptyList(),
    val canSilentDowngrade: Boolean = false,
    val pendingRollback: RollbackApkEntry? = null,
    val rollbackInProgress: Boolean = false,
    val confirmSignatureMismatchInstall: Boolean = false,
    /** M55: multi-user confirm; null when single-user / no silent cap. */
    val pendingDualUserAction: PendingDualUserAction? = null,
    /** One-shot: UI should launch system uninstall then clear. */
    val openSystemUninstall: Boolean = false,
    val activities: List<AppActivityInfo> = emptyList(),
    val services: List<AppComponentInfo> = emptyList(),
    val receivers: List<AppComponentInfo> = emptyList(),
    val providers: List<AppComponentInfo> = emptyList(),
    val activitiesLoading: Boolean = false,
    val activitiesQuery: String = "",
    val canToggleComponents: Boolean = false,
    val componentPackageDenylisted: Boolean = false,
    val pendingComponentToggle: PendingComponentToggle? = null,
    val componentToggleInProgress: Boolean = false,
) {
    val filteredActivities: List<AppActivityInfo>
        get() = activities.filter {
            AppActivityLabels.matchesQuery(
                AppActivityInfoQuery(label = it.label, className = it.className),
                activitiesQuery,
            )
        }

    val filteredServices: List<AppComponentInfo>
        get() = services.filter {
            AppComponentFilter.matchesQuery(it.label, it.className, activitiesQuery)
        }

    val filteredReceivers: List<AppComponentInfo>
        get() = receivers.filter {
            AppComponentFilter.matchesQuery(it.label, it.className, activitiesQuery)
        }

    val filteredProviders: List<AppComponentInfo>
        get() = providers.filter {
            AppComponentFilter.matchesQuery(it.label, it.className, activitiesQuery)
        }
}

sealed interface AppDetailEvent {
    data object Launch : AppDetailEvent
    data object OpenAppInfo : AppDetailEvent
    data object Uninstall : AppDetailEvent
    data object ClearOpenSystemUninstall : AppDetailEvent
    data class SelectDualUser(val userId: Int) : AppDetailEvent
    data object ConfirmDualUserAction : AppDetailEvent
    data object DismissDualUserAction : AppDetailEvent
    data object Export : AppDetailEvent
    data object Refresh : AppDetailEvent
    data class FormatSelected(val format: ApkExportFormat) : AppDetailEvent
    data object DismissExport : AppDetailEvent
    data object ClearSnackbar : AppDetailEvent
    data object InstallUpdate : AppDetailEvent
    data object RequestInstallDespiteSignature : AppDetailEvent
    data object ConfirmInstallDespiteSignature : AppDetailEvent
    data object DismissInstallDespiteSignature : AppDetailEvent
    data object OpenUnknownSourcesSettings : AppDetailEvent
    data class IgnoreUpdatesChanged(val checkForUpdates: Boolean) : AppDetailEvent
    data class UpdateCooldownChanged(val days: Int?) : AppDetailEvent
    data object OpenLinkSource : AppDetailEvent
    data object DismissLinkSource : AppDetailEvent
    data class LinkFdroidRepo(val repoId: String) : AppDetailEvent
    data class LinkGithub(val ownerRepo: String) : AppDetailEvent
    data class SetGitHubChannel(val channel: GitHubReleaseChannel) : AppDetailEvent
    data object RequestStopTracking : AppDetailEvent
    data object ConfirmStopTracking : AppDetailEvent
    data object DismissStopTracking : AppDetailEvent
    data class RequestRollback(val entry: RollbackApkEntry) : AppDetailEvent
    data object ConfirmRollback : AppDetailEvent
    data object DismissRollback : AppDetailEvent
    data class RequestComponentToggle(val component: AppComponentInfo, val enable: Boolean) : AppDetailEvent
    data object ConfirmComponentToggle : AppDetailEvent
    data object DismissComponentToggle : AppDetailEvent
}

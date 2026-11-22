package dev.involvex.kiln.feature.apps

import dev.involvex.kiln.core.model.ApkExportFormat
import dev.involvex.kiln.core.model.AppActivityInfo
import dev.involvex.kiln.core.model.AppUpdateInfo
import dev.involvex.kiln.core.model.GitHubReleaseChannel
import dev.involvex.kiln.core.model.InstallJobState
import dev.involvex.kiln.core.model.InstalledAppDetail
import dev.involvex.kiln.core.model.ManagedSourceLink
import dev.involvex.kiln.core.model.RepoConfig
import dev.involvex.kiln.core.model.RollbackApkEntry
import dev.involvex.kiln.feature.apps.activities.AppActivityInfoQuery
import dev.involvex.kiln.feature.apps.activities.AppActivityLabels

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
    val activities: List<AppActivityInfo> = emptyList(),
    val activitiesLoading: Boolean = false,
    val activitiesQuery: String = "",
) {
    val filteredActivities: List<AppActivityInfo>
        get() = activities.filter {
            AppActivityLabels.matchesQuery(
                AppActivityInfoQuery(label = it.label, className = it.className),
                activitiesQuery,
            )
        }
}

sealed interface AppDetailEvent {
    data object Launch : AppDetailEvent
    data object OpenAppInfo : AppDetailEvent
    data object Uninstall : AppDetailEvent
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
}

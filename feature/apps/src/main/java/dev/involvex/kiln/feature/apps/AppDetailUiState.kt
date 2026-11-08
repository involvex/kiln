package dev.involvex.kiln.feature.apps

import dev.involvex.kiln.core.model.ApkExportFormat
import dev.involvex.kiln.core.model.AppUpdateInfo
import dev.involvex.kiln.core.model.InstallJobState
import dev.involvex.kiln.core.model.InstalledAppDetail
import dev.involvex.kiln.core.model.RollbackApkEntry

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
    val rollbackEntries: List<RollbackApkEntry> = emptyList(),
    val canSilentDowngrade: Boolean = false,
    val pendingRollback: RollbackApkEntry? = null,
    val rollbackInProgress: Boolean = false,
)

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
    data object OpenUnknownSourcesSettings : AppDetailEvent
    data class IgnoreUpdatesChanged(val checkForUpdates: Boolean) : AppDetailEvent
    data class RequestRollback(val entry: RollbackApkEntry) : AppDetailEvent
    data object ConfirmRollback : AppDetailEvent
    data object DismissRollback : AppDetailEvent
}

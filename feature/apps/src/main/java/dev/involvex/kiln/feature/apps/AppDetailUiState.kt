package dev.involvex.kiln.feature.apps

import dev.involvex.kiln.core.model.ApkExportFormat
import dev.involvex.kiln.core.model.InstalledAppDetail

data class AppDetailUiState(
    val isLoading: Boolean = true,
    val detail: InstalledAppDetail? = null,
    val installerLabel: String? = null,
    val errorMessage: String? = null,
    val exportDialog: ExportDialogState? = null,
    val exportInProgress: Boolean = false,
    val snackbarMessage: String? = null,
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
}

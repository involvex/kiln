package dev.involvex.kiln.feature.apps

import android.net.Uri
import dev.involvex.kiln.core.model.ApkExportFormat
import dev.involvex.kiln.core.model.AppListFilter
import dev.involvex.kiln.core.model.AppSortField

sealed interface AppsEvent {
    data class QueryChanged(val value: String) : AppsEvent
    data class FilterChanged(val value: AppListFilter) : AppsEvent
    data class SortFieldChanged(val value: AppSortField) : AppsEvent
    data object SortDirectionToggled : AppsEvent
    data object Refresh : AppsEvent
    data class LongPressed(val packageName: String) : AppsEvent
    data class SelectionToggled(val packageName: String) : AppsEvent
    data object ClearSelection : AppsEvent
    data object ExportSelection : AppsEvent
    data class ExportFormatChanged(val format: ApkExportFormat) : AppsEvent
    data object DismissExport : AppsEvent
    data object CancelBatch : AppsEvent
    data object ClearBatchSummary : AppsEvent
    data object RequestNotificationPrompt : AppsEvent
    data object NotificationHandled : AppsEvent
    data class StartBatch(val treeUri: Uri) : AppsEvent
    data class SingleExportProgress(val inProgress: Boolean) : AppsEvent
}

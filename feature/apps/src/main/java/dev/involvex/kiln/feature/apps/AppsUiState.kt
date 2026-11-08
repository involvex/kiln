package dev.involvex.kiln.feature.apps

import dev.involvex.kiln.core.model.ApkExportFormat
import dev.involvex.kiln.core.model.AppListFilter
import dev.involvex.kiln.core.model.AppSortField
import dev.involvex.kiln.core.model.InstalledAppSummary
import dev.involvex.kiln.core.model.SortDirection

data class AppsUiState(
    val isLoading: Boolean = true,
    val apps: List<InstalledAppSummary> = emptyList(),
    val visibleApps: List<InstalledAppSummary> = emptyList(),
    val query: String = "",
    val filter: AppListFilter = AppListFilter.USER,
    val sortField: AppSortField = AppSortField.NAME,
    val sortDirection: SortDirection = SortDirection.ASCENDING,
    val selectedPackages: Set<String> = emptySet(),
    val selectionMode: Boolean = false,
    val errorMessage: String? = null,
    val exportDialog: ExportDialogState? = null,
    val singleExportProgress: Boolean = false,
    val batchExportRunning: Boolean = false,
    val batchProgressCurrent: Int = 0,
    val batchProgressTotal: Int = 0,
    val batchSummary: String? = null,
    val showNotificationPermissionRationale: Boolean = false,
    val pendingBatchAfterPermission: Boolean = false,
    /** packageName → available version label for update badge */
    val updateBadges: Map<String, String> = emptyMap(),
)

data class ExportDialogState(
    val packageNames: List<String>,
    val format: ApkExportFormat = ApkExportFormat.APK,
    val isBatch: Boolean,
    val warnBaseOnly: Boolean = false,
)

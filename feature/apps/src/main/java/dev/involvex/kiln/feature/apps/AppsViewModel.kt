package dev.involvex.kiln.feature.apps

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.core.model.ApkExportFormat
import dev.involvex.kiln.core.model.AppListFilter
import dev.involvex.kiln.core.model.AppSortField
import dev.involvex.kiln.core.model.AppUpdateRepository
import dev.involvex.kiln.core.model.InstalledAppDetail
import dev.involvex.kiln.core.model.InstalledAppSummary
import dev.involvex.kiln.core.model.SortDirection
import dev.involvex.kiln.feature.apps.data.InstalledAppsRepository
import dev.involvex.kiln.feature.apps.data.filterAndSortApps
import dev.involvex.kiln.feature.apps.export.ApkExporter
import dev.involvex.kiln.feature.apps.export.BatchExportScheduler
import dev.involvex.kiln.feature.apps.export.BatchExportWorker
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AppsViewModel @Inject constructor(
    private val repository: InstalledAppsRepository,
    private val batchExportScheduler: BatchExportScheduler,
    private val apkExporter: ApkExporter,
    private val appUpdateRepository: AppUpdateRepository,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(AppListFilter.USER)
    private val sortField = MutableStateFlow(AppSortField.NAME)
    private val sortDirection = MutableStateFlow(SortDirection.ASCENDING)
    private val selectedPackages = MutableStateFlow<Set<String>>(emptySet())
    private val selectionMode = MutableStateFlow(false)
    private val exportDialog = MutableStateFlow<ExportDialogState?>(null)
    private val singleExportProgress = MutableStateFlow(false)
    private val batchRunning = MutableStateFlow(false)
    private val batchProgressCurrent = MutableStateFlow(0)
    private val batchProgressTotal = MutableStateFlow(0)
    private val batchSummary = MutableStateFlow<String?>(null)
    private val showNotificationRationale = MutableStateFlow(false)

    private val listControls = combine(query, filter, sortField, sortDirection) { q, f, sf, sd ->
        ListControls(q, f, sf, sd)
    }

    private val selectionControls = combine(
        selectedPackages,
        selectionMode,
        exportDialog,
        singleExportProgress,
    ) { selected, selecting, dialog, singleProgress ->
        SelectionControls(selected, selecting, dialog, singleProgress)
    }

    private val batchControls = combine(
        batchRunning,
        batchProgressCurrent,
        batchProgressTotal,
        batchSummary,
        showNotificationRationale,
    ) { running, current, total, summary, rationale ->
        BatchControls(running, current, total, summary, rationale)
    }

    private val repoControls = combine(
        repository.summaries,
        repository.isLoading,
        repository.lastError,
    ) { apps, loading, error ->
        RepoControls(apps, loading, error)
    }

    private val updateBadges = MutableStateFlow<Map<String, String>>(emptyMap())

    val uiState: StateFlow<AppsUiState> = combine(
        repoControls,
        listControls,
        selectionControls,
        batchControls,
        updateBadges,
    ) { repo, list, selection, batch, badges ->
        val visible = filterAndSortApps(
            apps = repo.apps,
            query = list.query,
            filter = list.filter,
            sortField = list.sortField,
            sortDirection = list.sortDirection,
        )
        AppsUiState(
            isLoading = repo.loading && repo.apps.isEmpty(),
            apps = repo.apps,
            visibleApps = visible,
            query = list.query,
            filter = list.filter,
            sortField = list.sortField,
            sortDirection = list.sortDirection,
            selectedPackages = selection.selected,
            selectionMode = selection.selecting,
            errorMessage = repo.error,
            exportDialog = selection.dialog,
            singleExportProgress = selection.singleProgress,
            batchExportRunning = batch.running,
            batchProgressCurrent = batch.current,
            batchProgressTotal = batch.total,
            batchSummary = batch.summary,
            showNotificationPermissionRationale = batch.rationale,
            updateBadges = badges,
        )
    }.stateIn(
        scope = viewModelScope,
        // Eagerly so uiState.value stays current for export helpers / tests without a collector.
        started = SharingStarted.Eagerly,
        initialValue = AppsUiState(),
    )

    init {
        viewModelScope.launch {
            batchExportScheduler.observeWork().collect { infos ->
                handleWorkInfos(infos)
            }
        }
        viewModelScope.launch {
            appUpdateRepository.updates().collect { map ->
                updateBadges.value = map.mapValues { (_, info) ->
                    buildString {
                        append(info.availableVersionName)
                        if (!info.signatureMatches) append(" !")
                    }
                }
            }
        }
    }

    fun onEvent(event: AppsEvent) {
        when (event) {
            is AppsEvent.QueryChanged,
            is AppsEvent.FilterChanged,
            is AppsEvent.SortFieldChanged,
            AppsEvent.SortDirectionToggled,
            AppsEvent.Refresh,
            -> handleListEvent(event)

            is AppsEvent.LongPressed,
            is AppsEvent.SelectionToggled,
            AppsEvent.ClearSelection,
            AppsEvent.ExportSelection,
            -> handleSelectionEvent(event)

            is AppsEvent.ExportFormatChanged,
            AppsEvent.DismissExport,
            AppsEvent.CancelBatch,
            AppsEvent.ClearBatchSummary,
            AppsEvent.RequestNotificationPrompt,
            AppsEvent.NotificationHandled,
            is AppsEvent.StartBatch,
            is AppsEvent.SingleExportProgress,
            -> handleExportEvent(event)
        }
    }

    private fun handleListEvent(event: AppsEvent) {
        when (event) {
            is AppsEvent.QueryChanged -> query.value = event.value

            is AppsEvent.FilterChanged -> filter.value = event.value

            is AppsEvent.SortFieldChanged -> sortField.value = event.value

            AppsEvent.SortDirectionToggled -> {
                sortDirection.update {
                    if (it == SortDirection.ASCENDING) SortDirection.DESCENDING else SortDirection.ASCENDING
                }
            }

            AppsEvent.Refresh -> repository.refresh()

            else -> Unit
        }
    }

    private fun handleSelectionEvent(event: AppsEvent) {
        when (event) {
            is AppsEvent.LongPressed -> {
                selectionMode.value = true
                selectedPackages.update { it + event.packageName }
            }

            is AppsEvent.SelectionToggled -> toggleSelection(event.packageName)

            AppsEvent.ClearSelection -> clearSelection()

            AppsEvent.ExportSelection -> openExportForSelection()

            else -> Unit
        }
    }

    private fun handleExportEvent(event: AppsEvent) {
        when (event) {
            is AppsEvent.ExportFormatChanged -> exportDialog.update { it?.copy(format = event.format) }
            AppsEvent.DismissExport -> exportDialog.value = null
            AppsEvent.CancelBatch -> batchExportScheduler.cancel()
            AppsEvent.ClearBatchSummary -> batchSummary.value = null
            AppsEvent.RequestNotificationPrompt -> showNotificationRationale.value = true
            AppsEvent.NotificationHandled -> showNotificationRationale.value = false
            is AppsEvent.StartBatch -> startBatchExport(event.treeUri)
            is AppsEvent.SingleExportProgress -> singleExportProgress.value = event.inProgress
            else -> Unit
        }
    }

    suspend fun loadDetailForExport(packageName: String) = repository.detail(packageName)

    suspend fun exportToUri(
        detail: InstalledAppDetail,
        format: ApkExportFormat,
        uri: Uri,
        contentResolver: ContentResolver,
    ): Result<Unit> = apkExporter.exportToUri(detail, format, uri, contentResolver)

    suspend fun exportToFile(detail: InstalledAppDetail, format: ApkExportFormat, file: File): Result<Unit> =
        apkExporter.exportToFile(detail, format, file)

    private fun handleWorkInfos(infos: List<WorkInfo>) {
        val info = infos.firstOrNull()
        if (info == null) {
            batchRunning.value = false
            return
        }
        when (info.state) {
            WorkInfo.State.RUNNING, WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> {
                val progress = info.progress
                batchRunning.value = true
                batchProgressCurrent.value = progress.getInt(BatchExportWorker.KEY_PROGRESS_CURRENT, 0)
                batchProgressTotal.value = progress.getInt(BatchExportWorker.KEY_PROGRESS_TOTAL, 0)
                batchSummary.value = null
            }

            WorkInfo.State.SUCCEEDED -> {
                val success = info.outputData.getInt(BatchExportWorker.KEY_SUCCESS_COUNT, 0)
                val failed = info.outputData.getInt(BatchExportWorker.KEY_FAILED_COUNT, 0)
                batchRunning.value = false
                batchSummary.value = "ok=$success failed=$failed"
                clearSelection()
            }

            WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> {
                val cancelled = info.outputData.getBoolean(BatchExportWorker.KEY_CANCELLED, false)
                batchRunning.value = false
                batchSummary.value = if (cancelled) "cancelled" else "failed"
            }
        }
    }

    private fun toggleSelection(packageName: String) {
        if (!selectionMode.value) return
        selectedPackages.update { current ->
            if (packageName in current) current - packageName else current + packageName
        }
        if (selectedPackages.value.isEmpty()) {
            selectionMode.value = false
        }
    }

    private fun clearSelection() {
        selectedPackages.value = emptySet()
        selectionMode.value = false
    }

    private fun openExportForSelection() {
        val packages = selectedPackages.value.toList()
        if (packages.isEmpty()) return
        val warn = packages.any { pkg ->
            uiState.value.apps.firstOrNull { it.packageName == pkg }?.hasSplits == true
        }
        exportDialog.value = ExportDialogState(
            packageNames = packages,
            isBatch = packages.size > 1,
            warnBaseOnly = warn,
            format = if (warn) ApkExportFormat.APKS else ApkExportFormat.APK,
        )
    }

    private fun startBatchExport(treeUri: Uri) {
        val dialog = exportDialog.value ?: return
        batchExportScheduler.enqueue(
            treeUri = treeUri,
            packageNames = dialog.packageNames,
            format = dialog.format,
        )
        exportDialog.value = null
        batchRunning.value = true
        batchSummary.value = null
    }

    private data class ListControls(
        val query: String,
        val filter: AppListFilter,
        val sortField: AppSortField,
        val sortDirection: SortDirection,
    )

    private data class SelectionControls(
        val selected: Set<String>,
        val selecting: Boolean,
        val dialog: ExportDialogState?,
        val singleProgress: Boolean,
    )

    private data class BatchControls(val running: Boolean, val current: Int, val total: Int, val summary: String?, val rationale: Boolean)

    private data class RepoControls(val apps: List<InstalledAppSummary>, val loading: Boolean, val error: String?)
}

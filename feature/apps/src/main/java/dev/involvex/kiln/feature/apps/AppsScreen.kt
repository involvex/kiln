package dev.involvex.kiln.feature.apps

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.core.model.ApkExportFormat
import dev.involvex.kiln.core.model.InstalledAppDetail
import dev.involvex.kiln.core.model.SortDirection
import dev.involvex.kiln.feature.apps.export.buildExportFilename
import dev.involvex.kiln.feature.apps.ui.AppsListPane
import dev.involvex.kiln.feature.apps.ui.ExportFormatDialog
import dev.involvex.kiln.feature.apps.ui.NotificationRationaleDialog
import java.io.File
import kotlinx.coroutines.launch

@Composable
fun AppsRoute(
    onOpenDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
    embedded: Boolean = false,
    viewModel: AppsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AppsScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onOpenDetail = onOpenDetail,
        loadDetail = viewModel::loadDetailForExport,
        exportToUri = viewModel::exportToUri,
        exportToFile = viewModel::exportToFile,
        embedded = embedded,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
@Composable
fun AppsScreen(
    state: AppsUiState,
    onEvent: (AppsEvent) -> Unit,
    onOpenDetail: (String) -> Unit,
    loadDetail: suspend (String) -> Result<InstalledAppDetail>,
    exportToUri: suspend (InstalledAppDetail, ApkExportFormat, Uri, ContentResolver) -> Result<Unit>,
    exportToFile: suspend (InstalledAppDetail, ApkExportFormat, File) -> Result<Unit>,
    embedded: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val successText = stringResource(R.string.feature_apps_export_success)
    val failedTemplate = stringResource(R.string.feature_apps_export_failed)
    val exportActions = remember(state, onEvent, loadDetail, exportToUri, exportToFile) {
        AppsExportActions(
            state = state,
            onEvent = onEvent,
            loadDetail = loadDetail,
            exportToUri = exportToUri,
            exportToFile = exportToFile,
            context = context,
            snackbarHostState = snackbarHostState,
            successText = successText,
            failedTemplate = failedTemplate,
        )
    }

    val createDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri -> scope.launch { exportActions.onCreateDocument(uri) } }

    val openTree = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri -> exportActions.onTreePicked(uri) }

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        onEvent(AppsEvent.NotificationHandled)
        openTree.launch(null)
    }

    LaunchedEffect(state.batchSummary) {
        val summary = state.batchSummary ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(summary)
        onEvent(AppsEvent.ClearBatchSummary)
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = if (embedded) {
            WindowInsets(0, 0, 0, 0)
        } else {
            ScaffoldDefaults.contentWindowInsets
        },
        topBar = {
            if (embedded) {
                AppsActionsBar(state = state, onEvent = onEvent)
            } else {
                AppsTopBar(state = state, onEvent = onEvent)
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding: PaddingValues ->
        AppsListPane(
            state = state,
            onQueryChange = { onEvent(AppsEvent.QueryChanged(it)) },
            onFilterChange = { onEvent(AppsEvent.FilterChanged(it)) },
            onSortFieldChange = { onEvent(AppsEvent.SortFieldChanged(it)) },
            onRefresh = { onEvent(AppsEvent.Refresh) },
            onCheckUpdates = { onEvent(AppsEvent.CheckUpdates) },
            onDismissUpdateCheckMessage = { onEvent(AppsEvent.DismissUpdateCheckMessage) },
            onCancelBatch = { onEvent(AppsEvent.CancelBatch) },
            onOpenDetail = onOpenDetail,
            onLongPress = { onEvent(AppsEvent.LongPressed(it)) },
            onToggleSelection = { onEvent(AppsEvent.SelectionToggled(it)) },
            modifier = Modifier.padding(padding),
        )
    }

    state.exportDialog?.let { dialog ->
        ExportFormatDialog(
            state = dialog,
            inProgress = state.singleExportProgress,
            onFormatChange = { onEvent(AppsEvent.ExportFormatChanged(it)) },
            onDismiss = { onEvent(AppsEvent.DismissExport) },
            onConfirmSave = {
                if (dialog.isBatch) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        onEvent(AppsEvent.RequestNotificationPrompt)
                    } else {
                        openTree.launch(null)
                    }
                } else {
                    val packageName = dialog.packageNames.first()
                    val app = state.apps.firstOrNull { it.packageName == packageName }
                    createDocument.launch(
                        buildExportFilename(
                            label = app?.label ?: packageName,
                            versionName = app?.versionName,
                            versionCode = app?.versionCode ?: 0L,
                            format = dialog.format,
                        ),
                    )
                }
            },
            onConfirmShare = {
                if (!dialog.isBatch) {
                    scope.launch { exportActions.shareSingle(dialog.packageNames.first(), dialog.format) }
                }
            },
        )
    }

    if (state.showNotificationPermissionRationale) {
        NotificationRationaleDialog(
            onContinue = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    onEvent(AppsEvent.NotificationHandled)
                    openTree.launch(null)
                }
            },
            onSkip = {
                onEvent(AppsEvent.NotificationHandled)
                openTree.launch(null)
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppsTopBar(state: AppsUiState, onEvent: (AppsEvent) -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = if (state.selectionMode) {
                    stringResource(R.string.feature_apps_selected_count, state.selectedPackages.size)
                } else {
                    stringResource(R.string.feature_apps_title)
                },
            )
        },
        actions = { AppsActionButtons(state = state, onEvent = onEvent) },
    )
}

/** Sort / selection actions without a second title bar when the shell already shows segments. */
@Composable
private fun AppsActionsBar(state: AppsUiState, onEvent: (AppsEvent) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (state.selectionMode) {
            Text(
                text = stringResource(R.string.feature_apps_selected_count, state.selectedPackages.size),
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp),
            )
        }
        AppsActionButtons(state = state, onEvent = onEvent)
    }
}

@Composable
private fun AppsActionButtons(state: AppsUiState, onEvent: (AppsEvent) -> Unit) {
    if (state.selectionMode) {
        IconButton(onClick = { onEvent(AppsEvent.ExportSelection) }) {
            Icon(
                Icons.Filled.FileDownload,
                contentDescription = stringResource(R.string.feature_apps_export),
            )
        }
        IconButton(onClick = { onEvent(AppsEvent.ClearSelection) }) {
            Icon(
                Icons.Filled.Close,
                contentDescription = stringResource(R.string.feature_apps_clear_selection),
            )
        }
    } else {
        IconButton(onClick = { onEvent(AppsEvent.SortDirectionToggled) }) {
            Icon(
                Icons.AutoMirrored.Filled.Sort,
                contentDescription = if (state.sortDirection == SortDirection.ASCENDING) {
                    stringResource(R.string.feature_apps_sort_direction_asc)
                } else {
                    stringResource(R.string.feature_apps_sort_direction_desc)
                },
            )
        }
    }
}

private class AppsExportActions(
    private val state: AppsUiState,
    private val onEvent: (AppsEvent) -> Unit,
    private val loadDetail: suspend (String) -> Result<InstalledAppDetail>,
    private val exportToUri: suspend (InstalledAppDetail, ApkExportFormat, Uri, ContentResolver) -> Result<Unit>,
    private val exportToFile: suspend (InstalledAppDetail, ApkExportFormat, File) -> Result<Unit>,
    private val context: Context,
    private val snackbarHostState: SnackbarHostState,
    private val successText: String,
    private val failedTemplate: String,
) {
    suspend fun onCreateDocument(uri: Uri?) {
        val dialog = state.exportDialog ?: return
        if (uri == null || dialog.isBatch) return
        val packageName = dialog.packageNames.firstOrNull() ?: return
        onEvent(AppsEvent.SingleExportProgress(true))
        val detail = loadDetail(packageName).getOrNull()
        if (detail == null) {
            snackbarHostState.showSnackbar(failedTemplate.format("missing"))
            onEvent(AppsEvent.SingleExportProgress(false))
            onEvent(AppsEvent.DismissExport)
            return
        }
        val result = exportToUri(detail, dialog.format, uri, context.contentResolver)
        onEvent(AppsEvent.SingleExportProgress(false))
        onEvent(AppsEvent.DismissExport)
        val message = if (result.isSuccess) successText else failedTemplate.format(result.errorMessageOrNull() ?: "error")
        snackbarHostState.showSnackbar(message)
    }

    fun onTreePicked(uri: Uri?) {
        if (uri == null) {
            onEvent(AppsEvent.DismissExport)
            return
        }
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        runCatching { context.contentResolver.takePersistableUriPermission(uri, flags) }
        onEvent(AppsEvent.StartBatch(uri))
    }

    suspend fun shareSingle(packageName: String, format: ApkExportFormat) {
        onEvent(AppsEvent.SingleExportProgress(true))
        val detail = loadDetail(packageName).getOrNull()
        if (detail == null) {
            snackbarHostState.showSnackbar(failedTemplate.format("missing"))
            onEvent(AppsEvent.SingleExportProgress(false))
            onEvent(AppsEvent.DismissExport)
            return
        }
        val fileName = buildExportFilename(detail.label, detail.versionName, detail.versionCode, format)
        val outFile = File(File(context.cacheDir, "apk_export").apply { mkdirs() }, fileName)
        val result = exportToFile(detail, format, outFile)
        onEvent(AppsEvent.SingleExportProgress(false))
        onEvent(AppsEvent.DismissExport)
        if (result.isSuccess) {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
            val share = Intent(Intent.ACTION_SEND).apply {
                type = when (format) {
                    ApkExportFormat.APK -> "application/vnd.android.package-archive"
                    else -> "application/zip"
                }
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(share, null))
            snackbarHostState.showSnackbar(successText)
        } else {
            snackbarHostState.showSnackbar(failedTemplate.format(result.errorMessageOrNull() ?: "error"))
        }
    }
}

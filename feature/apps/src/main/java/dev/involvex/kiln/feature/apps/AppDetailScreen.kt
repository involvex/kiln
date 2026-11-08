package dev.involvex.kiln.feature.apps

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import dagger.hilt.android.EntryPointAccessors
import dev.involvex.kiln.core.designsystem.component.EmptyState
import dev.involvex.kiln.core.model.ApkExportFormat
import dev.involvex.kiln.feature.apps.di.ApkExporterEntryPoint
import dev.involvex.kiln.feature.apps.export.buildExportFilename
import dev.involvex.kiln.feature.apps.ui.AppDetailContent
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AppDetailRoute(
    packageName: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AppDetailViewModel = hiltViewModel<AppDetailViewModel, AppDetailViewModel.Factory>(
        creationCallback = { factory -> factory.create(packageName) },
    ),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AppDetailScreen(
        state = state,
        onBack = onBack,
        onEvent = viewModel::onEvent,
        setExportInProgress = viewModel::setExportInProgress,
        showMessage = viewModel::showMessage,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
fun AppDetailScreen(
    state: AppDetailUiState,
    onBack: () -> Unit,
    onEvent: (AppDetailEvent) -> Unit,
    setExportInProgress: (Boolean) -> Unit,
    showMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val successText = stringResource(R.string.feature_apps_export_success)
    val failedTemplate = stringResource(R.string.feature_apps_export_failed)
    val copiedText = stringResource(R.string.feature_apps_detail_copied)
    val apkExporter = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            ApkExporterEntryPoint::class.java,
        ).apkExporter()
    }

    val createDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        val dialog = state.exportDialog ?: return@rememberLauncherForActivityResult
        val detail = state.detail ?: return@rememberLauncherForActivityResult
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            setExportInProgress(true)
            val result = withContext(Dispatchers.IO) {
                apkExporter.exportToUri(detail, dialog.format, uri, context.contentResolver)
            }
            setExportInProgress(false)
            onEvent(AppDetailEvent.DismissExport)
            showMessage(
                if (result.isSuccess) successText else failedTemplate.format(result.errorMessageOrNull() ?: "error"),
            )
        }
    }

    LaunchedEffect(state.snackbarMessage) {
        val message = state.snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        onEvent(AppDetailEvent.ClearSnackbar)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(state.detail?.label ?: stringResource(R.string.feature_apps_detail_title))
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.feature_apps_back),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when {
            state.isLoading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                }
            }

            state.detail == null -> {
                EmptyState(
                    title = stringResource(R.string.feature_apps_error_title),
                    description = state.errorMessage.orEmpty(),
                    modifier = Modifier.padding(padding),
                )
            }

            else -> {
                AppDetailContent(
                    detail = state.detail,
                    installerLabel = state.installerLabel,
                    onCopied = { showMessage(copiedText) },
                    onExport = { onEvent(AppDetailEvent.Export) },
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }

    val dialog = state.exportDialog
    val detail = state.detail
    if (dialog != null && detail != null) {
        DetailExportDialog(
            dialog = dialog,
            inProgress = state.exportInProgress,
            onEvent = onEvent,
            onSave = {
                createDocument.launch(
                    buildExportFilename(detail.label, detail.versionName, detail.versionCode, dialog.format),
                )
            },
            onShare = {
                scope.launch {
                    setExportInProgress(true)
                    val fileName = buildExportFilename(
                        detail.label,
                        detail.versionName,
                        detail.versionCode,
                        dialog.format,
                    )
                    val outFile = File(File(context.cacheDir, "apk_export").apply { mkdirs() }, fileName)
                    val result = withContext(Dispatchers.IO) {
                        apkExporter.exportToFile(detail, dialog.format, outFile)
                    }
                    setExportInProgress(false)
                    onEvent(AppDetailEvent.DismissExport)
                    if (result.isSuccess) {
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            outFile,
                        )
                        val share = Intent(Intent.ACTION_SEND).apply {
                            type = when (dialog.format) {
                                ApkExportFormat.APK -> "application/vnd.android.package-archive"
                                else -> "application/zip"
                            }
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(share, null))
                    } else {
                        showMessage(failedTemplate.format(result.errorMessageOrNull() ?: "error"))
                    }
                }
            },
        )
    }
}

@Composable
private fun DetailExportDialog(
    dialog: ExportDialogState,
    inProgress: Boolean,
    onEvent: (AppDetailEvent) -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!inProgress) onEvent(AppDetailEvent.DismissExport) },
        title = { Text(stringResource(R.string.feature_apps_export_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (dialog.warnBaseOnly) {
                    Text(stringResource(R.string.feature_apps_export_split_warning))
                }
                FormatOption(ApkExportFormat.APK, dialog.format, onEvent, stringResource(R.string.feature_apps_export_apk))
                FormatOption(ApkExportFormat.APKS, dialog.format, onEvent, stringResource(R.string.feature_apps_export_apks))
                FormatOption(ApkExportFormat.XAPK, dialog.format, onEvent, stringResource(R.string.feature_apps_export_xapk))
                if (inProgress) CircularProgressIndicator()
            }
        },
        confirmButton = {
            TextButton(enabled = !inProgress, onClick = onSave) {
                Text(stringResource(R.string.feature_apps_export_confirm))
            }
        },
        dismissButton = {
            Row {
                TextButton(enabled = !inProgress, onClick = onShare) {
                    Text(stringResource(R.string.feature_apps_export_share))
                }
                TextButton(enabled = !inProgress, onClick = { onEvent(AppDetailEvent.DismissExport) }) {
                    Text(stringResource(R.string.feature_apps_export_cancel))
                }
            }
        },
    )
}

@Composable
private fun FormatOption(format: ApkExportFormat, selected: ApkExportFormat, onEvent: (AppDetailEvent) -> Unit, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(
            selected = selected == format,
            onClick = { onEvent(AppDetailEvent.FormatSelected(format)) },
        )
        Text(label)
    }
}

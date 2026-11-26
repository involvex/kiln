package dev.involvex.kiln.feature.apps

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.EntryPointAccessors
import dev.involvex.kiln.core.designsystem.component.EmptyState
import dev.involvex.kiln.core.designsystem.kilnHiltViewModel
import dev.involvex.kiln.core.model.ApkExportFormat
import dev.involvex.kiln.core.model.RepoConfig
import dev.involvex.kiln.feature.apps.di.ApkExporterEntryPoint
import dev.involvex.kiln.feature.apps.export.buildExportFilename
import dev.involvex.kiln.feature.apps.ui.AppDetailContent
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AppDetailRoute(packageName: String, onBack: () -> Unit, onOpenPrivilegeSetup: () -> Unit = {}, modifier: Modifier = Modifier) {
    val viewModel = kilnHiltViewModel<AppDetailViewModel>()
    LaunchedEffect(packageName) {
        viewModel.load(packageName)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AppDetailScreen(
        state = state,
        onBack = onBack,
        onEvent = viewModel::onEvent,
        setExportInProgress = viewModel::setExportInProgress,
        showMessage = viewModel::showMessage,
        onActivitiesQueryChange = viewModel::setActivitiesQuery,
        onOpenPrivilegeSetup = onOpenPrivilegeSetup,
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
    onActivitiesQueryChange: (String) -> Unit,
    onOpenPrivilegeSetup: () -> Unit = {},
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

    val rollbackOk = stringResource(R.string.feature_apps_rollback_ok)
    val rollbackNeedsPrivilege = stringResource(R.string.feature_apps_rollback_needs_privilege)
    val linkOk = stringResource(R.string.feature_apps_link_ok)
    val badGithub = stringResource(R.string.feature_apps_link_bad_github)
    val unlinked = stringResource(R.string.feature_apps_unlinked)
    val unlinkFailed = stringResource(R.string.feature_apps_unlink_failed)
    val activityStartNotFound = stringResource(R.string.feature_apps_activities_start_not_found)
    val activityStartSecurity = stringResource(R.string.feature_apps_activities_start_security)
    val activityShortcutRequested = stringResource(R.string.feature_apps_activities_shortcut_requested)
    val activityShortcutRejected = stringResource(R.string.feature_apps_activities_shortcut_rejected)
    val activityShortcutUnsupported = stringResource(R.string.feature_apps_activities_shortcut_unsupported)
    val componentEnabled = stringResource(R.string.feature_apps_components_enabled)
    val componentDisabled = stringResource(R.string.feature_apps_components_disabled)
    val componentNeedsPrivilege = stringResource(R.string.feature_apps_components_need_privilege)
    val componentDenylisted = stringResource(R.string.feature_apps_components_denylisted)
    val componentNotExported = stringResource(R.string.feature_apps_components_not_exported)
    val componentToggleBlocked = stringResource(R.string.feature_apps_components_toggle_blocked)
    val uninstallOk = stringResource(R.string.feature_apps_uninstall_ok)
    LaunchedEffect(state.snackbarMessage) {
        val raw = state.snackbarMessage ?: return@LaunchedEffect
        val message = when (raw) {
            "rollback_ok" -> rollbackOk
            "rollback_needs_privilege" -> rollbackNeedsPrivilege
            "link_ok" -> linkOk
            "bad_github" -> badGithub
            "unlinked" -> unlinked
            "unlink_failed" -> unlinkFailed
            "activity_start_not_found" -> activityStartNotFound
            "activity_start_security" -> activityStartSecurity
            "activity_shortcut_requested" -> activityShortcutRequested
            "activity_shortcut_rejected" -> activityShortcutRejected
            "activity_shortcut_unsupported" -> activityShortcutUnsupported
            "component_enabled" -> componentEnabled
            "component_disabled" -> componentDisabled
            "component_needs_privilege" -> componentNeedsPrivilege
            "component_denylisted" -> componentDenylisted
            "component_not_exported" -> componentNotExported
            "component_toggle_blocked" -> componentToggleBlocked
            "uninstall_ok" -> uninstallOk
            else -> raw
        }
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
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                )
            }

            else -> {
                AppDetailContent(
                    detail = state.detail,
                    installerLabel = state.installerLabel,
                    onCopied = { showMessage(copiedText) },
                    onExport = { onEvent(AppDetailEvent.Export) },
                    onUninstall = { onEvent(AppDetailEvent.Uninstall) },
                    availableUpdate = state.availableUpdate,
                    installJob = state.installJob,
                    ignoreUpdates = state.ignoreUpdates,
                    onIgnoreUpdatesChanged = { onEvent(AppDetailEvent.IgnoreUpdatesChanged(it)) },
                    updateCooldownDays = state.updateCooldownDays,
                    globalCooldownDays = state.globalCooldownDays,
                    onUpdateCooldownChanged = { onEvent(AppDetailEvent.UpdateCooldownChanged(it)) },
                    managedLink = state.managedLink,
                    onOpenLinkSource = { onEvent(AppDetailEvent.OpenLinkSource) },
                    onStopTracking = { onEvent(AppDetailEvent.RequestStopTracking) },
                    onGitHubChannelChange = { onEvent(AppDetailEvent.SetGitHubChannel(it)) },
                    hasGitHubToken = state.hasGitHubToken,
                    onInstallUpdate = { onEvent(AppDetailEvent.InstallUpdate) },
                    onRequestInstallDespiteSignature = {
                        onEvent(AppDetailEvent.RequestInstallDespiteSignature)
                    },
                    onOpenUnknownSources = {
                        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    },
                    rollbackEntries = state.rollbackEntries,
                    canSilentDowngrade = state.canSilentDowngrade,
                    rollbackInProgress = state.rollbackInProgress,
                    onRequestRollback = { onEvent(AppDetailEvent.RequestRollback(it)) },
                    activities = state.filteredActivities,
                    services = state.filteredServices,
                    receivers = state.filteredReceivers,
                    providers = state.filteredProviders,
                    activitiesLoading = state.activitiesLoading,
                    activitiesQuery = state.activitiesQuery,
                    onActivitiesQueryChange = onActivitiesQueryChange,
                    showActivitiesSearch = state.activities.size > ACTIVITIES_SEARCH_THRESHOLD,
                    onActivityMessage = showMessage,
                    canToggleComponents = state.canToggleComponents,
                    componentPackageDenylisted = state.componentPackageDenylisted,
                    componentToggleInProgress = state.componentToggleInProgress,
                    onRequestComponentToggle = { component, enable ->
                        onEvent(AppDetailEvent.RequestComponentToggle(component, enable))
                    },
                    onOpenPrivilegeSetup = onOpenPrivilegeSetup,
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }

    state.pendingComponentToggle?.let { pending ->
        AlertDialog(
            onDismissRequest = { onEvent(AppDetailEvent.DismissComponentToggle) },
            title = {
                Text(
                    stringResource(
                        if (pending.enable) {
                            R.string.feature_apps_components_enable_title
                        } else {
                            R.string.feature_apps_components_disable_title
                        },
                    ),
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(
                            R.string.feature_apps_components_toggle_body,
                            pending.component.label,
                            pending.component.className,
                        ),
                    )
                    if (pending.users.size >= 2) {
                        Text(
                            stringResource(R.string.feature_apps_dual_user_body_component),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        pending.users.forEach { user ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                RadioButton(
                                    selected = pending.selectedUserId == user.id,
                                    onClick = {
                                        onEvent(AppDetailEvent.SelectComponentToggleUser(user.id))
                                    },
                                )
                                Text(
                                    text = stringResource(
                                        R.string.feature_apps_dual_user_option,
                                        user.name,
                                        user.id,
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { onEvent(AppDetailEvent.ConfirmComponentToggle) }) {
                    Text(stringResource(R.string.feature_apps_components_toggle_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(AppDetailEvent.DismissComponentToggle) }) {
                    Text(stringResource(R.string.feature_apps_components_toggle_cancel))
                }
            },
        )
    }

    if (state.confirmSignatureMismatchInstall) {
        AlertDialog(
            onDismissRequest = { onEvent(AppDetailEvent.DismissInstallDespiteSignature) },
            title = { Text(stringResource(R.string.feature_apps_install_anyway_title)) },
            text = { Text(stringResource(R.string.feature_apps_install_anyway_body)) },
            confirmButton = {
                TextButton(onClick = { onEvent(AppDetailEvent.ConfirmInstallDespiteSignature) }) {
                    Text(stringResource(R.string.feature_apps_install_anyway_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(AppDetailEvent.DismissInstallDespiteSignature) }) {
                    Text(stringResource(R.string.feature_apps_install_anyway_cancel))
                }
            },
        )
    }

    if (state.pendingDualUserAction == null) {
        state.pendingRollback?.let { pending ->
            AlertDialog(
                onDismissRequest = { onEvent(AppDetailEvent.DismissRollback) },
                title = { Text(stringResource(R.string.feature_apps_rollback_confirm_title)) },
                text = {
                    Text(
                        stringResource(
                            R.string.feature_apps_rollback_confirm_body,
                            pending.versionName,
                            pending.versionCode,
                        ),
                    )
                },
                confirmButton = {
                    TextButton(onClick = { onEvent(AppDetailEvent.ConfirmRollback) }) {
                        Text(stringResource(R.string.feature_apps_rollback_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { onEvent(AppDetailEvent.DismissRollback) }) {
                        Text(stringResource(R.string.feature_apps_rollback_cancel))
                    }
                },
            )
        }
    }

    state.pendingDualUserAction?.let { pending ->
        AlertDialog(
            onDismissRequest = { onEvent(AppDetailEvent.DismissDualUserAction) },
            title = { Text(stringResource(R.string.feature_apps_dual_user_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.feature_apps_dual_user_body))
                    pending.users.forEach { user ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            RadioButton(
                                selected = pending.selectedUserId == user.id,
                                onClick = { onEvent(AppDetailEvent.SelectDualUser(user.id)) },
                            )
                            Text(
                                text = stringResource(
                                    R.string.feature_apps_dual_user_option,
                                    user.name,
                                    user.id,
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { onEvent(AppDetailEvent.ConfirmDualUserAction) }) {
                    Text(stringResource(R.string.feature_apps_dual_user_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(AppDetailEvent.DismissDualUserAction) }) {
                    Text(stringResource(R.string.feature_apps_dual_user_cancel))
                }
            },
        )
    }

    LaunchedEffect(state.openSystemUninstall, state.detail?.packageName) {
        val pkg = state.detail?.packageName
        if (state.openSystemUninstall && pkg != null) {
            context.startActivity(
                Intent(Intent.ACTION_DELETE).apply {
                    data = Uri.fromParts("package", pkg, null)
                },
            )
            onEvent(AppDetailEvent.ClearOpenSystemUninstall)
        }
    }

    if (state.showLinkSourceDialog) {
        AppLinkSourceDialog(
            appLabel = state.detail?.label.orEmpty(),
            initialGithub = state.managedLink?.githubOwnerRepo.orEmpty(),
            enabledRepos = state.enabledRepos,
            savedGithubRepos = state.savedGithubRepos,
            onDismiss = { onEvent(AppDetailEvent.DismissLinkSource) },
            onLinkFdroid = { onEvent(AppDetailEvent.LinkFdroidRepo(it)) },
            onLinkGithub = { onEvent(AppDetailEvent.LinkGithub(it)) },
        )
    }

    if (state.showStopTrackingConfirm) {
        AlertDialog(
            onDismissRequest = { onEvent(AppDetailEvent.DismissStopTracking) },
            title = {
                Text(
                    stringResource(
                        R.string.feature_apps_stop_tracking_title,
                        state.detail?.label.orEmpty(),
                    ),
                )
            },
            text = { Text(stringResource(R.string.feature_apps_stop_tracking_body)) },
            confirmButton = {
                TextButton(onClick = { onEvent(AppDetailEvent.ConfirmStopTracking) }) {
                    Text(stringResource(R.string.feature_apps_stop_tracking_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(AppDetailEvent.DismissStopTracking) }) {
                    Text(stringResource(R.string.feature_apps_stop_tracking_cancel))
                }
            },
        )
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppLinkSourceDialog(
    appLabel: String,
    initialGithub: String,
    enabledRepos: List<RepoConfig>,
    savedGithubRepos: List<String>,
    onDismiss: () -> Unit,
    onLinkFdroid: (String) -> Unit,
    onLinkGithub: (String) -> Unit,
) {
    var githubInput by remember(initialGithub) { mutableStateOf(initialGithub) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.feature_apps_link_dialog_title, appLabel)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.feature_apps_link_dialog_body), style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.feature_apps_link_fdroid_section), style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    enabledRepos.forEach { repo ->
                        AssistChip(onClick = { onLinkFdroid(repo.id) }, label = { Text(repo.name) })
                    }
                }
                if (enabledRepos.isEmpty()) {
                    Text(
                        stringResource(R.string.feature_apps_link_no_repos),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    stringResource(R.string.feature_apps_link_saved_github_section),
                    style = MaterialTheme.typography.titleSmall,
                )
                if (savedGithubRepos.isEmpty()) {
                    Text(
                        stringResource(R.string.feature_apps_link_no_saved_github),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        savedGithubRepos.forEach { ownerRepo ->
                            AssistChip(onClick = { onLinkGithub(ownerRepo) }, label = { Text(ownerRepo) })
                        }
                    }
                }
                Text(stringResource(R.string.feature_apps_link_github_section), style = MaterialTheme.typography.titleSmall)
                OutlinedTextField(
                    value = githubInput,
                    onValueChange = { githubInput = it },
                    label = { Text(stringResource(R.string.feature_apps_link_github_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onLinkGithub(githubInput) },
                enabled = githubInput.contains('/'),
            ) {
                Text(stringResource(R.string.feature_apps_link_github_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.feature_apps_link_cancel))
            }
        },
    )
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

private const val ACTIVITIES_SEARCH_THRESHOLD = 8

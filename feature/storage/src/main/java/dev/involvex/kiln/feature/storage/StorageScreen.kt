package dev.involvex.kiln.feature.storage

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.involvex.kiln.core.common.ByteSizeFormatter
import dev.involvex.kiln.core.designsystem.component.KilnScaffold
import dev.involvex.kiln.core.designsystem.scroll.ScrollToTopEffect
import dev.involvex.kiln.core.model.ApkLeftover
import dev.involvex.kiln.core.model.AppStorageStats
import dev.involvex.kiln.core.model.OrphanStorageDir

@Composable
fun StorageRoute(
    modifier: Modifier = Modifier,
    showTopBar: Boolean = true,
    onOpenPrivilegeSetup: () -> Unit = {},
    viewModel: StorageViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val treePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri: Uri? ->
        if (uri != null) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, flags)
            }
            viewModel.scanLeftovers(uri)
        }
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissionAndLoad()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    StorageScreen(
        state = state,
        onOpenUsageAccess = {
            context.startActivity(viewModel.usageAccessIntent())
        },
        onOpenPrivilegeSetup = onOpenPrivilegeSetup,
        onRefresh = viewModel::load,
        onIncludeSystem = viewModel::setIncludeSystem,
        onQueryChange = viewModel::setQuery,
        onClearApp = viewModel::requestClearApp,
        onClearAll = viewModel::requestClearAllCaches,
        onConfirmClear = {
            val pending = state.pendingClear
            if (pending != null && !state.canClearPrivileged) {
                val pkg = pending.packageName
                if (pkg != null) {
                    context.startActivity(viewModel.appDetailsIntent(pkg))
                }
                viewModel.dismissClear()
            } else {
                viewModel.confirmClear()
            }
        },
        onDismissClear = viewModel::dismissClear,
        onOpenAppDetails = { pkg ->
            context.startActivity(viewModel.appDetailsIntent(pkg))
        },
        onPickLeftoverFolder = { treePicker.launch(null) },
        onToggleLeftover = viewModel::toggleLeftover,
        onRequestDeleteLeftovers = viewModel::requestDeleteSelectedLeftovers,
        onRefreshOrphans = viewModel::loadOrphans,
        onToggleOrphan = viewModel::toggleOrphan,
        onRequestDeleteOrphans = viewModel::requestDeleteSelectedOrphans,
        onConfirmDelete = viewModel::confirmDelete,
        onDismissDelete = viewModel::dismissDelete,
        onClearMessage = viewModel::clearMessage,
        showTopBar = showTopBar,
        modifier = modifier,
    )
}

@Composable
@Suppress("LongParameterList", "LongMethod")
fun StorageScreen(
    state: StorageUiState,
    onOpenUsageAccess: () -> Unit,
    onOpenPrivilegeSetup: () -> Unit,
    onRefresh: () -> Unit,
    onIncludeSystem: (Boolean) -> Unit,
    onQueryChange: (String) -> Unit,
    onClearApp: (AppStorageStats) -> Unit,
    onClearAll: () -> Unit,
    onConfirmClear: () -> Unit,
    onDismissClear: () -> Unit,
    onOpenAppDetails: (String) -> Unit,
    onPickLeftoverFolder: () -> Unit,
    onToggleLeftover: (String) -> Unit,
    onRequestDeleteLeftovers: () -> Unit,
    onRefreshOrphans: () -> Unit,
    onToggleOrphan: (OrphanStorageDir) -> Unit,
    onRequestDeleteOrphans: () -> Unit,
    onConfirmDelete: () -> Unit,
    onDismissDelete: () -> Unit,
    onClearMessage: () -> Unit,
    modifier: Modifier = Modifier,
    showTopBar: Boolean = true,
) {
    KilnScaffold(
        title = stringResource(R.string.feature_storage_title),
        modifier = modifier,
        showTopBar = showTopBar,
    ) { padding ->
        when (state.permission) {
            StoragePermissionState.UNKNOWN,
            StoragePermissionState.MISSING,
            -> UsageAccessGate(
                padding = padding,
                onOpenUsageAccess = onOpenUsageAccess,
            )

            StoragePermissionState.GRANTED -> StorageContent(
                padding = padding,
                state = state,
                onRefresh = onRefresh,
                onIncludeSystem = onIncludeSystem,
                onQueryChange = onQueryChange,
                onClearApp = onClearApp,
                onClearAll = onClearAll,
                onOpenAppDetails = onOpenAppDetails,
                onOpenPrivilegeSetup = onOpenPrivilegeSetup,
                onPickLeftoverFolder = onPickLeftoverFolder,
                onToggleLeftover = onToggleLeftover,
                onRequestDeleteLeftovers = onRequestDeleteLeftovers,
                onRefreshOrphans = onRefreshOrphans,
                onToggleOrphan = onToggleOrphan,
                onRequestDeleteOrphans = onRequestDeleteOrphans,
                onClearMessage = onClearMessage,
            )
        }
        state.pendingClear?.let { pending ->
            ClearCacheDialog(
                pending = pending,
                privileged = state.canClearPrivileged,
                clearing = state.clearing,
                onConfirm = onConfirmClear,
                onDismiss = onDismissClear,
            )
        }
        state.pendingDelete?.let { pending ->
            val body = when (pending) {
                is PendingStorageDelete.Leftovers -> stringResource(
                    R.string.storage_confirm_delete_leftovers,
                    pending.items.size,
                )

                is PendingStorageDelete.Orphans -> stringResource(
                    R.string.storage_confirm_delete_orphans,
                    pending.items.size,
                )
            }
            AlertDialog(
                onDismissRequest = { if (!state.deleting) onDismissDelete() },
                title = { Text(stringResource(R.string.storage_confirm_title)) },
                text = { Text(body) },
                confirmButton = {
                    TextButton(onClick = onConfirmDelete, enabled = !state.deleting) {
                        Text(stringResource(R.string.storage_delete_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismissDelete, enabled = !state.deleting) {
                        Text(stringResource(R.string.storage_cancel))
                    }
                },
            )
        }
    }
}

@Composable
private fun UsageAccessGate(padding: PaddingValues, onOpenUsageAccess: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.storage_usage_required_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.storage_usage_required_body),
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(onClick = onOpenUsageAccess, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.storage_open_usage_access))
        }
    }
}

@Composable
@Suppress("LongParameterList", "LongMethod")
private fun StorageContent(
    padding: PaddingValues,
    state: StorageUiState,
    onRefresh: () -> Unit,
    onIncludeSystem: (Boolean) -> Unit,
    onQueryChange: (String) -> Unit,
    onClearApp: (AppStorageStats) -> Unit,
    onClearAll: () -> Unit,
    onOpenAppDetails: (String) -> Unit,
    onOpenPrivilegeSetup: () -> Unit,
    onPickLeftoverFolder: () -> Unit,
    onToggleLeftover: (String) -> Unit,
    onRequestDeleteLeftovers: () -> Unit,
    onRefreshOrphans: () -> Unit,
    onToggleOrphan: (OrphanStorageDir) -> Unit,
    onRequestDeleteOrphans: () -> Unit,
    onClearMessage: () -> Unit,
) {
    if (state.isLoading && state.apps.isEmpty()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator()
        }
        return
    }
    val listState = rememberLazyListState()
    ScrollToTopEffect(listState)
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { StorageTotals(state = state) }
        item { SizeBars(apps = state.sizeBarApps) }
        item { StorageToolbar(state, onRefresh, onIncludeSystem, onClearAll, onQueryChange) }
        item {
            LeftoversSection(
                state = state,
                onPickFolder = onPickLeftoverFolder,
                onToggle = onToggleLeftover,
                onDelete = onRequestDeleteLeftovers,
            )
        }
        item {
            OrphansSection(
                state = state,
                onOpenPrivilegeSetup = onOpenPrivilegeSetup,
                onRefresh = onRefreshOrphans,
                onToggle = onToggleOrphan,
                onDelete = onRequestDeleteOrphans,
            )
        }
        state.message?.let { message ->
            item {
                val text = when {
                    message == "capability_missing" -> stringResource(R.string.storage_capability_missing)

                    message.startsWith("done:") -> {
                        val parts = message.removePrefix("done:").split(':')
                        stringResource(
                            R.string.storage_result_fmt,
                            parts.getOrNull(0)?.toIntOrNull() ?: 0,
                            parts.getOrNull(1)?.toIntOrNull() ?: 0,
                        )
                    }

                    else -> message
                }
                Text(text = text, style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = onClearMessage) {
                    Text(stringResource(R.string.storage_cancel))
                }
            }
        }
        state.error?.let { err ->
            item { Text(text = err, color = MaterialTheme.colorScheme.error) }
        }
        items(state.filteredApps, key = { it.packageName }) { app ->
            StorageAppRow(
                app = app,
                canClearPrivileged = state.canClearPrivileged,
                onClear = { onClearApp(app) },
                onOpenDetails = { onOpenAppDetails(app.packageName) },
            )
        }
    }
}

private const val MIN_BAR_FRACTION = 0.02f

@Composable
private fun SizeBars(apps: List<AppStorageStats>) {
    if (apps.isEmpty()) return
    val max = apps.maxOf { it.totalBytes }.coerceAtLeast(1L)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.storage_size_bars_title),
            style = MaterialTheme.typography.titleSmall,
        )
        apps.forEach { app ->
            val fraction = (app.totalBytes.toFloat() / max.toFloat()).coerceIn(MIN_BAR_FRACTION, 1f)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "${app.label} · ${ByteSizeFormatter.format(app.totalBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                )
                Row(Modifier.fillMaxWidth().height(10.dp)) {
                    Box(
                        Modifier
                            .weight(fraction)
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primary),
                    )
                    if (fraction < 1f) {
                        Box(
                            Modifier
                                .weight(1f - fraction)
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LeftoversSection(state: StorageUiState, onPickFolder: () -> Unit, onToggle: (String) -> Unit, onDelete: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.storage_leftovers_title), style = MaterialTheme.typography.titleSmall)
        Text(
            stringResource(R.string.storage_leftovers_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(onClick = onPickFolder, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.storage_leftovers_pick))
        }
        if (state.leftoversLoading) {
            CircularProgressIndicator()
        } else if (state.leftovers.isEmpty()) {
            Text(stringResource(R.string.storage_leftovers_empty), style = MaterialTheme.typography.bodySmall)
        } else {
            state.leftovers.forEach { item ->
                LeftoverRow(
                    item = item,
                    selected = item.id in state.selectedLeftoverIds,
                    onToggle = { onToggle(item.id) },
                )
            }
            OutlinedButton(
                onClick = onDelete,
                enabled = state.selectedLeftoverIds.isNotEmpty() && !state.deleting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.storage_leftovers_delete))
            }
        }
    }
}

@Composable
private fun LeftoverRow(item: ApkLeftover, selected: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = selected, onCheckedChange = { onToggle() })
        Text(
            text = stringResource(
                R.string.storage_leftovers_item,
                item.displayName,
                ByteSizeFormatter.format(item.sizeBytes),
            ),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

@Composable
private fun OrphansSection(
    state: StorageUiState,
    onOpenPrivilegeSetup: () -> Unit,
    onRefresh: () -> Unit,
    onToggle: (OrphanStorageDir) -> Unit,
    onDelete: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.storage_orphans_title), style = MaterialTheme.typography.titleSmall)
        Text(
            stringResource(R.string.storage_orphans_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!state.canAccessAndroidData) {
            OutlinedButton(onClick = onOpenPrivilegeSetup, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.storage_orphans_privilege))
            }
        } else {
            OutlinedButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.storage_orphans_refresh))
            }
            when {
                state.orphansLoading -> CircularProgressIndicator()

                state.orphansError != null -> Text(
                    text = state.orphansError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )

                state.orphans.isEmpty() -> Text(
                    stringResource(R.string.storage_orphans_empty),
                    style = MaterialTheme.typography.bodySmall,
                )

                else -> {
                    state.orphans.forEach { orphan ->
                        val key = state.orphanKey(orphan)
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onToggle(orphan) },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = key in state.selectedOrphans,
                                onCheckedChange = { onToggle(orphan) },
                            )
                            Text(
                                text = stringResource(
                                    R.string.storage_orphans_item,
                                    orphan.root.pathKey,
                                    orphan.packageName,
                                    ByteSizeFormatter.format(orphan.sizeBytes),
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 4.dp),
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = onDelete,
                        enabled = state.selectedOrphans.isNotEmpty() && !state.deleting,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.storage_orphans_delete))
                    }
                }
            }
        }
    }
}

@Composable
private fun StorageToolbar(
    state: StorageUiState,
    onRefresh: () -> Unit,
    onIncludeSystem: (Boolean) -> Unit,
    onClearAll: () -> Unit,
    onQueryChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!state.canClearPrivileged) {
            Text(
                text = stringResource(R.string.storage_privilege_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(
                selected = !state.includeSystem,
                onClick = { onIncludeSystem(false) },
                label = { Text(stringResource(R.string.storage_filter_user)) },
            )
            FilterChip(
                selected = state.includeSystem,
                onClick = { onIncludeSystem(true) },
                label = { Text(stringResource(R.string.storage_filter_all)) },
            )
            OutlinedButton(onClick = onRefresh) {
                Text(stringResource(R.string.storage_refresh))
            }
        }
        OutlinedButton(
            onClick = onClearAll,
            enabled = state.totalCacheBytes > 0 && state.canClearPrivileged,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                stringResource(
                    R.string.storage_clear_all_caches,
                    ByteSizeFormatter.format(state.totalCacheBytes),
                ),
            )
        }
        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(stringResource(R.string.storage_search)) },
        )
    }
}

@Composable
private fun StorageTotals(state: StorageUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(
                R.string.storage_total_line,
                ByteSizeFormatter.format(state.totalBytes),
            ),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(
                R.string.storage_breakdown_line,
                ByteSizeFormatter.format(state.totalAppBytes),
                ByteSizeFormatter.format(state.totalDataBytes),
                ByteSizeFormatter.format(state.totalCacheBytes),
            ),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun StorageAppRow(app: AppStorageStats, canClearPrivileged: Boolean, onClear: () -> Unit, onOpenDetails: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenDetails)
            .padding(vertical = 6.dp),
    ) {
        Text(text = app.label, style = MaterialTheme.typography.titleSmall)
        Text(
            text = app.packageName,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(
                R.string.storage_app_breakdown,
                ByteSizeFormatter.format(app.totalBytes),
                ByteSizeFormatter.format(app.appBytes),
                ByteSizeFormatter.format(app.dataBytes),
                ByteSizeFormatter.format(app.cacheBytes),
            ),
            style = MaterialTheme.typography.bodySmall,
        )
        TextButton(onClick = onClear, enabled = app.cacheBytes > 0 || !canClearPrivileged) {
            Text(
                if (canClearPrivileged) {
                    stringResource(R.string.storage_clear_cache)
                } else {
                    stringResource(R.string.storage_open_app_info)
                },
            )
        }
    }
}

@Composable
private fun ClearCacheDialog(
    pending: PendingClearAction,
    privileged: Boolean,
    clearing: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val body = when {
        pending.packageName == null -> stringResource(
            R.string.storage_confirm_clear_all,
            ByteSizeFormatter.format(pending.cacheBytes),
        )

        privileged -> stringResource(
            R.string.storage_confirm_clear_app,
            pending.label,
            ByteSizeFormatter.format(pending.cacheBytes),
        )

        else -> stringResource(R.string.storage_confirm_open_settings, pending.label)
    }
    AlertDialog(
        onDismissRequest = { if (!clearing) onDismiss() },
        title = { Text(stringResource(R.string.storage_confirm_title)) },
        text = { Text(body) },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !clearing) {
                Text(stringResource(R.string.storage_confirm_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !clearing) {
                Text(stringResource(R.string.storage_cancel))
            }
        },
    )
}

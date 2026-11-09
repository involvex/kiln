package dev.involvex.kiln.feature.storage

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import dev.involvex.kiln.core.model.AppStorageStats

@Composable
fun StorageRoute(modifier: Modifier = Modifier, showTopBar: Boolean = true, viewModel: StorageViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
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
        showTopBar = showTopBar,
        modifier = modifier,
    )
}

@Composable
fun StorageScreen(
    state: StorageUiState,
    onOpenUsageAccess: () -> Unit,
    onRefresh: () -> Unit,
    onIncludeSystem: (Boolean) -> Unit,
    onQueryChange: (String) -> Unit,
    onClearApp: (AppStorageStats) -> Unit,
    onClearAll: () -> Unit,
    onConfirmClear: () -> Unit,
    onDismissClear: () -> Unit,
    onOpenAppDetails: (String) -> Unit,
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
private fun StorageContent(
    padding: PaddingValues,
    state: StorageUiState,
    onRefresh: () -> Unit,
    onIncludeSystem: (Boolean) -> Unit,
    onQueryChange: (String) -> Unit,
    onClearApp: (AppStorageStats) -> Unit,
    onClearAll: () -> Unit,
    onOpenAppDetails: (String) -> Unit,
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
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { StorageTotals(state = state) }
        item { StorageToolbar(state, onRefresh, onIncludeSystem, onClearAll, onQueryChange) }
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

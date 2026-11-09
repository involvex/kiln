package dev.involvex.kiln.feature.permissions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.involvex.kiln.core.designsystem.component.KilnScaffold
import dev.involvex.kiln.core.model.PermissionAuditApp

@Composable
fun PermissionAuditRoute(
    onBack: () -> Unit,
    onOpenPrivilegeSetup: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PermissionAuditViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    PermissionAuditScreen(
        state = state,
        onBack = onBack,
        onOpenPrivilegeSetup = onOpenPrivilegeSetup,
        onRefresh = viewModel::refresh,
        onQueryChange = viewModel::setQuery,
        onOpenDetail = viewModel::openDetail,
        onCloseDetail = viewModel::closeDetail,
        onTogglePermission = viewModel::togglePermission,
        onSelectAll = viewModel::selectAllDangerousForDetail,
        onRequestRevokeSelected = viewModel::requestRevokeSelected,
        onRequestRevokeAll = viewModel::requestRevokeAllForDetail,
        onDismissRevoke = viewModel::dismissRevokeConfirm,
        onConfirmRevoke = viewModel::confirmRevoke,
        onAppOpChange = viewModel::setAppOpSelection,
        onAppOpModeChange = viewModel::setAppOpMode,
        onRequestAppOp = viewModel::requestSetAppOp,
        onDismissAppOp = viewModel::dismissAppOpConfirm,
        onConfirmAppOp = viewModel::confirmSetAppOp,
        onClearMessage = viewModel::clearMessage,
        modifier = modifier,
    )
}

@Composable
fun PermissionAuditScreen(
    state: PermissionAuditUiState,
    onBack: () -> Unit,
    onOpenPrivilegeSetup: () -> Unit,
    onRefresh: () -> Unit,
    onQueryChange: (String) -> Unit,
    onOpenDetail: (String) -> Unit,
    onCloseDetail: () -> Unit,
    onTogglePermission: (String) -> Unit,
    onSelectAll: () -> Unit,
    onRequestRevokeSelected: () -> Unit,
    onRequestRevokeAll: () -> Unit,
    onDismissRevoke: () -> Unit,
    onConfirmRevoke: () -> Unit,
    onAppOpChange: (String) -> Unit,
    onAppOpModeChange: (String) -> Unit,
    onRequestAppOp: () -> Unit,
    onDismissAppOp: () -> Unit,
    onConfirmAppOp: () -> Unit,
    onClearMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    KilnScaffold(title = stringResource(R.string.permissions_title), modifier = modifier) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.permissions_back))
            }
            if (!state.canMutate) {
                Text(
                    text = stringResource(R.string.permissions_privilege_hint),
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedButton(onClick = onOpenPrivilegeSetup, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.permissions_open_privilege))
                }
            }
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.permissions_search)) },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onRefresh) {
                    Text(stringResource(R.string.permissions_refresh))
                }
            }
            state.message?.let { message ->
                val text = when {
                    message == "capability_missing" -> stringResource(R.string.permissions_capability_missing)

                    message.startsWith("done:") -> {
                        val parts = message.removePrefix("done:").split(':')
                        stringResource(
                            R.string.permissions_result_fmt,
                            parts.getOrNull(0)?.toIntOrNull() ?: 0,
                            parts.getOrNull(1)?.toIntOrNull() ?: 0,
                        )
                    }

                    else -> message
                }
                Text(text = text, style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = onClearMessage) {
                    Text(stringResource(R.string.permissions_cancel))
                }
            }
            when {
                state.isLoading && state.apps.isEmpty() -> {
                    Column(
                        Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CircularProgressIndicator()
                        Text(stringResource(R.string.permissions_loading))
                    }
                }

                state.detailApp != null -> {
                    DetailPane(
                        state = state,
                        app = state.detailApp!!,
                        onCloseDetail = onCloseDetail,
                        onTogglePermission = onTogglePermission,
                        onSelectAll = onSelectAll,
                        onRequestRevokeSelected = onRequestRevokeSelected,
                        onRequestRevokeAll = onRequestRevokeAll,
                        onAppOpChange = onAppOpChange,
                        onAppOpModeChange = onAppOpModeChange,
                        onRequestAppOp = onRequestAppOp,
                    )
                }

                state.filteredApps.isEmpty() -> {
                    Text(stringResource(R.string.permissions_empty))
                }

                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(state.filteredApps, key = { it.packageName }) { app ->
                            AppRow(app = app, onClick = { onOpenDetail(app.packageName) })
                        }
                    }
                }
            }
        }
    }

    state.pendingRevoke?.let { pending ->
        AlertDialog(
            onDismissRequest = onDismissRevoke,
            title = { Text(stringResource(R.string.permissions_confirm_revoke_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.permissions_confirm_revoke_body,
                        pending.targets.size,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = onConfirmRevoke, enabled = !state.running) {
                    Text(stringResource(R.string.permissions_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissRevoke) {
                    Text(stringResource(R.string.permissions_cancel))
                }
            },
        )
    }

    state.pendingAppOp?.let { pending ->
        AlertDialog(
            onDismissRequest = onDismissAppOp,
            title = { Text(stringResource(R.string.permissions_confirm_appops_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.permissions_confirm_appops_body,
                        pending.op,
                        pending.mode,
                        pending.packageName,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = onConfirmAppOp, enabled = !state.running) {
                    Text(stringResource(R.string.permissions_confirm_appops))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissAppOp) {
                    Text(stringResource(R.string.permissions_cancel))
                }
            },
        )
    }
}

@Composable
private fun AppRow(app: PermissionAuditApp, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
    ) {
        Text(text = app.label, style = MaterialTheme.typography.titleMedium)
        Text(text = app.packageName, style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.permissions_count_fmt, app.dangerousGrantedCount),
                style = MaterialTheme.typography.bodySmall,
            )
            if (app.isSystemApp) {
                Text(
                    text = stringResource(R.string.permissions_system_badge),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun DetailPane(
    state: PermissionAuditUiState,
    app: PermissionAuditApp,
    onCloseDetail: () -> Unit,
    onTogglePermission: (String) -> Unit,
    onSelectAll: () -> Unit,
    onRequestRevokeSelected: () -> Unit,
    onRequestRevokeAll: () -> Unit,
    onAppOpChange: (String) -> Unit,
    onAppOpModeChange: (String) -> Unit,
    onRequestAppOp: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = app.label, style = MaterialTheme.typography.titleLarge)
        Text(text = app.packageName, style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = onCloseDetail) {
            Text(stringResource(R.string.permissions_close_detail))
        }
        Text(
            text = stringResource(R.string.permissions_detail_title),
            style = MaterialTheme.typography.titleMedium,
        )
        app.dangerousGranted.forEach { entry ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onTogglePermission(entry.name) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = entry.name in state.selectedPermissions,
                    onCheckedChange = { onTogglePermission(entry.name) },
                    enabled = state.canMutate,
                )
                Text(
                    text = entry.name,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onSelectAll, enabled = state.canMutate) {
                Text(stringResource(R.string.permissions_select))
            }
            OutlinedButton(
                onClick = onRequestRevokeSelected,
                enabled = state.canMutate && state.selectedPermissions.isNotEmpty() && !state.running,
            ) {
                Text(stringResource(R.string.permissions_revoke_selected))
            }
        }
        OutlinedButton(
            onClick = onRequestRevokeAll,
            enabled = state.canMutate && !state.running,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.permissions_revoke_all_for_app))
        }
        HorizontalDivider()
        Text(
            text = stringResource(R.string.permissions_appops_section),
            style = MaterialTheme.typography.titleMedium,
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            state.allowedAppOps.forEach { op ->
                FilterChip(
                    selected = state.selectedAppOp == op,
                    onClick = { onAppOpChange(op) },
                    label = { Text(op) },
                    enabled = state.canMutate,
                )
            }
        }
        Text(stringResource(R.string.permissions_appops_mode))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            state.allowedModes.forEach { mode ->
                FilterChip(
                    selected = state.selectedAppOpMode == mode,
                    onClick = { onAppOpModeChange(mode) },
                    label = { Text(mode) },
                    enabled = state.canMutate,
                )
            }
        }
        OutlinedButton(
            onClick = onRequestAppOp,
            enabled = state.canMutate && !state.running,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.permissions_appops_apply))
        }
    }
}

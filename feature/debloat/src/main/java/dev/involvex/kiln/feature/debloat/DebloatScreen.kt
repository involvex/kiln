package dev.involvex.kiln.feature.debloat

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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import dev.involvex.kiln.core.model.DebloatAction
import dev.involvex.kiln.core.model.DebloatAppRow
import dev.involvex.kiln.core.model.DebloatPackageState
import dev.involvex.kiln.core.model.DebloatRating
import dev.involvex.kiln.feature.debloat.policy.DebloatPolicy

@Composable
fun DebloatRoute(
    onBack: () -> Unit,
    onOpenPrivilegeSetup: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DebloatViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DebloatScreen(
        state = state,
        onBack = onBack,
        onOpenPrivilegeSetup = onOpenPrivilegeSetup,
        onRefresh = viewModel::refresh,
        onQueryChange = viewModel::setQuery,
        onFilter = viewModel::setFilter,
        onSystemOnly = viewModel::setSystemOnly,
        onExpertMode = viewModel::setExpertMode,
        onBatchAction = viewModel::setBatchAction,
        onToggle = viewModel::toggle,
        onRequestApply = viewModel::requestApply,
        onDismissConfirm = viewModel::dismissConfirm,
        onConfirmText = viewModel::setConfirmText,
        onConfirmApply = viewModel::confirmApply,
        onCreateSnapshot = viewModel::createSnapshot,
        onRestoreSnapshot = viewModel::restoreSnapshot,
        modifier = modifier,
    )
}

@Composable
fun DebloatScreen(
    state: DebloatUiState,
    onBack: () -> Unit,
    onOpenPrivilegeSetup: () -> Unit,
    onRefresh: () -> Unit,
    onQueryChange: (String) -> Unit,
    onFilter: (DebloatFilterState) -> Unit,
    onSystemOnly: (Boolean) -> Unit,
    onExpertMode: (Boolean) -> Unit,
    onBatchAction: (DebloatAction) -> Unit,
    onToggle: (String) -> Unit,
    onRequestApply: () -> Unit,
    onDismissConfirm: () -> Unit,
    onConfirmText: (String) -> Unit,
    onConfirmApply: () -> Unit,
    onCreateSnapshot: () -> Unit,
    onRestoreSnapshot: () -> Unit,
    modifier: Modifier = Modifier,
) {
    KilnScaffold(title = stringResource(R.string.debloat_title), modifier = modifier) { padding ->
        if (!state.hasDebloatCapability) {
            DebloatPrivilegeGate(
                padding = padding,
                onOpenPrivilegeSetup = onOpenPrivilegeSetup,
                onBack = onBack,
            )
            return@KilnScaffold
        }
        if (state.isLoading && state.rows.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
            }
            return@KilnScaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                DebloatToolbar(
                    state = state,
                    onRefresh = onRefresh,
                    onQueryChange = onQueryChange,
                    onFilter = onFilter,
                    onSystemOnly = onSystemOnly,
                    onExpertMode = onExpertMode,
                    onBatchAction = onBatchAction,
                    onRequestApply = onRequestApply,
                    onCreateSnapshot = onCreateSnapshot,
                    onRestoreSnapshot = onRestoreSnapshot,
                    onBack = onBack,
                )
            }
            items(state.visibleRows, key = { it.packageName }) { row ->
                DebloatRow(
                    row = row,
                    selected = row.packageName in state.selected,
                    canSelect = DebloatPolicy.canSelect(row, state.batchAction, state.expertMode),
                    onToggle = { onToggle(row.packageName) },
                )
            }
        }
        if (state.pendingConfirm) {
            DebloatConfirmDialog(
                state = state,
                onDismiss = onDismissConfirm,
                onConfirmText = onConfirmText,
                onConfirm = onConfirmApply,
            )
        }
    }
}

@Composable
private fun DebloatPrivilegeGate(padding: PaddingValues, onOpenPrivilegeSetup: () -> Unit, onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(padding).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.debloat_need_shizuku_title), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.debloat_need_shizuku_body), style = MaterialTheme.typography.bodyMedium)
        Button(onClick = onOpenPrivilegeSetup, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.debloat_open_privilege))
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.debloat_back))
        }
    }
}

@Composable
private fun DebloatToolbar(
    state: DebloatUiState,
    onRefresh: () -> Unit,
    onQueryChange: (String) -> Unit,
    onFilter: (DebloatFilterState) -> Unit,
    onSystemOnly: (Boolean) -> Unit,
    onExpertMode: (Boolean) -> Unit,
    onBatchAction: (DebloatAction) -> Unit,
    onRequestApply: () -> Unit,
    onCreateSnapshot: () -> Unit,
    onRestoreSnapshot: () -> Unit,
    onBack: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.debloat_intro), style = MaterialTheme.typography.bodySmall)
        Text(
            text = if (state.snapshotAtEpochMs == null) {
                stringResource(R.string.debloat_no_snapshot)
            } else {
                stringResource(R.string.debloat_snapshot_present)
            },
            style = MaterialTheme.typography.bodySmall,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onCreateSnapshot) { Text(stringResource(R.string.debloat_snapshot)) }
            OutlinedButton(
                onClick = onRestoreSnapshot,
                enabled = state.snapshotAtEpochMs != null && !state.running,
            ) {
                Text(stringResource(R.string.debloat_restore))
            }
            OutlinedButton(onClick = onRefresh) { Text(stringResource(R.string.debloat_refresh)) }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.debloat_system_only), modifier = Modifier.weight(1f))
            Switch(checked = state.systemOnly, onCheckedChange = onSystemOnly)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.debloat_expert_mode), modifier = Modifier.weight(1f))
            Switch(checked = state.expertMode, onCheckedChange = onExpertMode)
        }
        if (!state.expertMode) {
            Text(
                stringResource(R.string.debloat_expert_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.batchAction == DebloatAction.DISABLE,
                onClick = { onBatchAction(DebloatAction.DISABLE) },
                label = { Text(stringResource(R.string.debloat_action_disable)) },
            )
            FilterChip(
                selected = state.batchAction == DebloatAction.UNINSTALL_FOR_USER,
                onClick = { onBatchAction(DebloatAction.UNINSTALL_FOR_USER) },
                label = { Text(stringResource(R.string.debloat_action_uninstall)) },
            )
            FilterChip(
                selected = state.batchAction == DebloatAction.ENABLE ||
                    state.batchAction == DebloatAction.INSTALL_EXISTING,
                onClick = { onBatchAction(DebloatAction.ENABLE) },
                label = { Text(stringResource(R.string.debloat_action_restore_pkg)) },
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.filter == DebloatFilterState.ALL,
                onClick = { onFilter(DebloatFilterState.ALL) },
                label = { Text(stringResource(R.string.debloat_filter_all)) },
            )
            FilterChip(
                selected = state.filter == DebloatFilterState.ENABLED,
                onClick = { onFilter(DebloatFilterState.ENABLED) },
                label = { Text(stringResource(R.string.debloat_filter_enabled)) },
            )
            FilterChip(
                selected = state.filter == DebloatFilterState.DISABLED,
                onClick = { onFilter(DebloatFilterState.DISABLED) },
                label = { Text(stringResource(R.string.debloat_filter_disabled)) },
            )
        }
        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(stringResource(R.string.debloat_search)) },
        )
        Button(
            onClick = onRequestApply,
            enabled = state.selected.isNotEmpty() && !state.running,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.debloat_apply, state.selected.size))
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.debloat_back))
        }
        if (state.running) {
            CircularProgressIndicator()
        }
        state.message?.let {
            Text(text = it, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun DebloatRow(row: DebloatAppRow, selected: Boolean, canSelect: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = canSelect, onClick = onToggle)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = selected, onCheckedChange = { if (canSelect) onToggle() }, enabled = canSelect)
        Column(modifier = Modifier.weight(1f)) {
            Text(row.label, style = MaterialTheme.typography.titleSmall)
            Text(row.packageName, style = MaterialTheme.typography.bodySmall)
            Text(
                text = stringResource(
                    R.string.debloat_row_meta,
                    stateLabel(row.state),
                    ratingLabel(row.rating),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (row.denylisted) {
                Text(
                    stringResource(R.string.debloat_denylisted, row.denylistReason ?: ""),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun DebloatConfirmDialog(state: DebloatUiState, onDismiss: () -> Unit, onConfirmText: (String) -> Unit, onConfirm: () -> Unit) {
    val typedOk = !state.needsTypedConfirm || DebloatViewModel.isTypedConfirmValid(state.confirmText)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.debloat_confirm_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(
                        R.string.debloat_confirm_body,
                        state.selected.size,
                        actionLabel(state.batchAction),
                    ),
                )
                state.selectedRows.take(12).forEach { row ->
                    Text("• ${row.label} (${row.rating.name})", style = MaterialTheme.typography.bodySmall)
                }
                if (state.selectedRows.size > 12) {
                    Text(stringResource(R.string.debloat_confirm_more, state.selectedRows.size - 12))
                }
                if (state.needsTypedConfirm) {
                    Text(stringResource(R.string.debloat_type_confirm_hint))
                    OutlinedTextField(
                        value = state.confirmText,
                        onValueChange = onConfirmText,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = typedOk) {
                Text(stringResource(R.string.debloat_confirm_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.debloat_cancel)) }
        },
    )
}

@Composable
private fun stateLabel(state: DebloatPackageState): String = when (state) {
    DebloatPackageState.ENABLED -> stringResource(R.string.debloat_state_enabled)
    DebloatPackageState.DISABLED -> stringResource(R.string.debloat_state_disabled)
    DebloatPackageState.UNINSTALLED_FOR_USER -> stringResource(R.string.debloat_state_uninstalled)
}

@Composable
private fun ratingLabel(rating: DebloatRating): String = when (rating) {
    DebloatRating.RECOMMENDED -> stringResource(R.string.debloat_rating_recommended)
    DebloatRating.ADVANCED -> stringResource(R.string.debloat_rating_advanced)
    DebloatRating.EXPERT -> stringResource(R.string.debloat_rating_expert)
    DebloatRating.UNSAFE -> stringResource(R.string.debloat_rating_unsafe)
    DebloatRating.UNKNOWN -> stringResource(R.string.debloat_rating_unknown)
}

@Composable
private fun actionLabel(action: DebloatAction): String = when (action) {
    DebloatAction.DISABLE -> stringResource(R.string.debloat_action_disable)
    DebloatAction.ENABLE -> stringResource(R.string.debloat_action_restore_pkg)
    DebloatAction.UNINSTALL_FOR_USER -> stringResource(R.string.debloat_action_uninstall)
    DebloatAction.INSTALL_EXISTING -> stringResource(R.string.debloat_action_restore_pkg)
}

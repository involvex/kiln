package dev.involvex.kiln.feature.permissions.processes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.involvex.kiln.core.designsystem.component.KilnScaffold
import dev.involvex.kiln.core.designsystem.kilnHiltViewModel
import dev.involvex.kiln.core.model.ProcessStat
import dev.involvex.kiln.feature.permissions.R

@Composable
fun ProcessesRoute(onBack: () -> Unit, onOpenPrivilegeSetup: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel = kilnHiltViewModel<ProcessesViewModel>()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ProcessesScreen(
        state = state,
        onBack = onBack,
        onOpenPrivilegeSetup = onOpenPrivilegeSetup,
        onRefresh = viewModel::refresh,
        onRequestForceStop = viewModel::requestForceStop,
        onConfirmForceStop = viewModel::confirmForceStop,
        onDismissForceStop = viewModel::dismissForceStop,
        modifier = modifier,
    )
}

@Composable
@Suppress("LongParameterList")
fun ProcessesScreen(
    state: ProcessesUiState,
    onBack: () -> Unit,
    onOpenPrivilegeSetup: () -> Unit,
    onRefresh: () -> Unit,
    onRequestForceStop: (String) -> Unit,
    onConfirmForceStop: () -> Unit,
    onDismissForceStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    KilnScaffold(title = stringResource(R.string.processes_title), modifier = modifier) { padding ->
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
            Text(
                stringResource(R.string.processes_explain),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!state.canList) {
                Text(
                    stringResource(R.string.processes_need_shizuku),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedButton(onClick = onOpenPrivilegeSetup, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.processes_open_setup))
                }
            } else {
                OutlinedButton(onClick = onRefresh, enabled = !state.isLoading, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.processes_refresh))
                }
            }
            when {
                state.message == "force_stop_ok" -> {
                    Text(
                        stringResource(R.string.processes_force_stop_ok),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                state.message == "force_stop_blocked" -> {
                    Text(
                        stringResource(R.string.processes_force_stop_blocked),
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                !state.message.isNullOrBlank() -> {
                    Text(state.message.orEmpty(), color = MaterialTheme.colorScheme.error)
                }

                !state.errorKey.isNullOrBlank() -> {
                    Text(
                        stringResource(R.string.processes_error, state.errorKey.orEmpty()),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            if (state.canList && state.processes.isEmpty() && !state.isLoading) {
                Text(
                    stringResource(R.string.processes_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(state.processes, key = { it.packageName }) { row ->
                    ProcessRow(
                        row = row,
                        canForceStop = state.canForceStop,
                        onForceStop = { onRequestForceStop(row.packageName) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
    state.pendingKillPackage?.let { pkg ->
        AlertDialog(
            onDismissRequest = onDismissForceStop,
            title = { Text(stringResource(R.string.processes_force_stop_title)) },
            text = { Text(stringResource(R.string.processes_force_stop_body, pkg)) },
            confirmButton = {
                TextButton(onClick = onConfirmForceStop) {
                    Text(stringResource(R.string.processes_force_stop_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissForceStop) {
                    Text(stringResource(R.string.processes_force_stop_cancel))
                }
            },
        )
    }
}

@Composable
private fun ProcessRow(row: ProcessStat, canForceStop: Boolean, onForceStop: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(row.label, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                row.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                stringResource(R.string.processes_mem_line, formatPssMb(row.pssKb)),
                style = MaterialTheme.typography.labelSmall,
            )
        }
        if (canForceStop) {
            TextButton(onClick = onForceStop) {
                Text(stringResource(R.string.processes_force_stop))
            }
        }
    }
}

private fun formatPssMb(pssKb: Long): String {
    val mb = pssKb / 1024.0
    return String.format("%.1f", mb)
}

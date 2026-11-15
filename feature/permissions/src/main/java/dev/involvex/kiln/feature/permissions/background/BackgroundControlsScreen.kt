package dev.involvex.kiln.feature.permissions.background

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.involvex.kiln.core.designsystem.component.KilnScaffold
import dev.involvex.kiln.core.model.BackgroundControlsApp
import dev.involvex.kiln.core.model.BatteryUsageMode
import dev.involvex.kiln.feature.permissions.R

@Composable
fun BackgroundControlsRoute(
    onBack: () -> Unit,
    onOpenPrivilegeSetup: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BackgroundControlsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BackgroundControlsScreen(
        state = state,
        onBack = onBack,
        onOpenPrivilegeSetup = onOpenPrivilegeSetup,
        onRefresh = viewModel::refresh,
        onQueryChange = viewModel::setQuery,
        onIncludeSystemChange = viewModel::setIncludeSystem,
        onAutostartChange = viewModel::setAutostart,
        onBatteryModeChange = viewModel::setBatteryMode,
        onClearMessage = viewModel::clearMessage,
        modifier = modifier,
    )
}

@Composable
fun BackgroundControlsScreen(
    state: BackgroundControlsUiState,
    onBack: () -> Unit,
    onOpenPrivilegeSetup: () -> Unit,
    onRefresh: () -> Unit,
    onQueryChange: (String) -> Unit,
    onIncludeSystemChange: (Boolean) -> Unit,
    onAutostartChange: (String, Boolean) -> Unit,
    onBatteryModeChange: (String, BatteryUsageMode) -> Unit,
    onClearMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    KilnScaffold(title = stringResource(R.string.background_controls_title), modifier = modifier) { padding ->
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
                stringResource(R.string.background_controls_explain),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!state.canMutate) {
                Text(
                    stringResource(R.string.background_controls_need_shizuku),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedButton(onClick = onOpenPrivilegeSetup, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.background_controls_open_setup))
                }
            }
            if (!state.isHyperOs) {
                Text(
                    stringResource(R.string.background_controls_autostart_xiaomi_only),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.background_controls_search)) },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !state.includeSystem,
                    onClick = { onIncludeSystemChange(false) },
                    label = { Text(stringResource(R.string.background_controls_filter_user)) },
                )
                FilterChip(
                    selected = state.includeSystem,
                    onClick = { onIncludeSystemChange(true) },
                    label = { Text(stringResource(R.string.background_controls_filter_all)) },
                )
                TextButton(onClick = onRefresh) {
                    Text(stringResource(R.string.background_controls_refresh))
                }
            }
            if (state.message != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = state.message,
                        modifier = Modifier.weight(1f),
                        color = if (state.messageIsError) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    )
                    TextButton(onClick = onClearMessage) {
                        Text(stringResource(R.string.permissions_dismiss))
                    }
                }
            }
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(state.filteredApps, key = { it.packageName }) { app ->
                        BackgroundControlsRow(
                            app = app,
                            showAutostart = state.isHyperOs,
                            canMutate = state.canMutate,
                            busy = state.mutatingPackage == app.packageName,
                            onAutostartChange = { onAutostartChange(app.packageName, it) },
                            onBatteryModeChange = { onBatteryModeChange(app.packageName, it) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BackgroundControlsRow(
    app: BackgroundControlsApp,
    showAutostart: Boolean,
    canMutate: Boolean,
    busy: Boolean,
    onAutostartChange: (Boolean) -> Unit,
    onBatteryModeChange: (BatteryUsageMode) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        Text(app.label, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            app.packageName,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (showAutostart) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(stringResource(R.string.background_controls_autostart))
                if (busy) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Switch(
                        checked = app.autostartAllowed == true,
                        onCheckedChange = onAutostartChange,
                        enabled = canMutate && app.autostartAllowed != null,
                    )
                }
            }
        }
        Text(stringResource(R.string.background_controls_battery), style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            BatteryUsageMode.entries.forEach { mode ->
                FilterChip(
                    selected = app.batteryMode == mode,
                    onClick = { onBatteryModeChange(mode) },
                    enabled = canMutate && !busy && app.batteryMode != null,
                    label = { Text(batteryLabel(mode)) },
                )
            }
        }
    }
}

@Composable
private fun batteryLabel(mode: BatteryUsageMode): String = stringResource(
    when (mode) {
        BatteryUsageMode.UNRESTRICTED -> R.string.background_controls_battery_unrestricted
        BatteryUsageMode.OPTIMIZED -> R.string.background_controls_battery_optimized
        BatteryUsageMode.RESTRICTED -> R.string.background_controls_battery_restricted
    },
)

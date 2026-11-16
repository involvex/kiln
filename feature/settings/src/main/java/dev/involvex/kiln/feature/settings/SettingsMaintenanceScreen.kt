package dev.involvex.kiln.feature.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.involvex.kiln.core.designsystem.kilnHiltViewModel
import dev.involvex.kiln.core.model.MaintenanceRunRecord
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.delay

@Composable
fun SettingsMaintenanceRoute(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel = kilnHiltViewModel<SettingsViewModel>()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsCategoryScaffold(
        title = stringResource(R.string.settings_hub_maintenance),
        onBack = onBack,
        modifier = modifier,
    ) { padding ->
        SettingsScrollColumn(padding = padding) {
            Text(
                text = stringResource(R.string.settings_maintenance_explain),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SettingsSwitchRow(
                title = stringResource(R.string.settings_maintenance_enabled),
                checked = state.maintenance.enabled,
                onCheckedChange = viewModel::setMaintenanceEnabled,
            )
            if (state.maintenance.enabled) {
                SettingsSwitchRow(
                    title = stringResource(R.string.settings_maintenance_trim),
                    checked = state.maintenance.trimCaches,
                    onCheckedChange = viewModel::setMaintenanceTrimCaches,
                )
                SettingsSwitchRow(
                    title = stringResource(R.string.settings_maintenance_downloads),
                    checked = state.maintenance.pruneDownloads,
                    onCheckedChange = viewModel::setMaintenancePruneDownloads,
                )
                SettingsSwitchRow(
                    title = stringResource(R.string.settings_maintenance_rollback),
                    checked = state.maintenance.pruneRollback,
                    onCheckedChange = viewModel::setMaintenancePruneRollback,
                )
                SettingsSwitchRow(
                    title = stringResource(R.string.settings_maintenance_backup_temp),
                    checked = state.maintenance.pruneBackupTemp,
                    onCheckedChange = viewModel::setMaintenancePruneBackupTemp,
                )
            }
            OutlinedButton(
                onClick = viewModel::runMaintenanceNow,
                enabled = state.maintenance.enabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.settings_maintenance_run_now))
            }
            if (state.maintenanceRunQueued) {
                Text(
                    text = stringResource(R.string.settings_maintenance_run_queued),
                    style = MaterialTheme.typography.bodySmall,
                )
                LaunchedEffect(state.maintenanceRunQueued) {
                    delay(2_500)
                    viewModel.consumeMaintenanceRunQueued()
                }
            }
            Text(
                text = lastRunSummary(state.lastMaintenanceRun),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun lastRunSummary(lastRun: MaintenanceRunRecord?): String {
    if (lastRun == null) {
        return stringResource(R.string.settings_maintenance_last_run_none)
    }
    val whenText = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
        .format(Date(lastRun.finishedAtEpochMs))
    val status = if (lastRun.ok) {
        stringResource(R.string.settings_maintenance_last_run_ok)
    } else {
        stringResource(R.string.settings_maintenance_last_run_partial)
    }
    val lines = lastRun.tasks.joinToString("; ") { "${it.taskId}=${it.status}" }
    return stringResource(R.string.settings_maintenance_last_run, whenText, status, lines)
}

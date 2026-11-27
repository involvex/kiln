package dev.involvex.kiln.feature.permissions.diagnostics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
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
import dev.involvex.kiln.core.model.BatteryChargeStatus
import dev.involvex.kiln.core.model.BatteryHealth
import dev.involvex.kiln.core.model.BatteryPowerSource
import dev.involvex.kiln.core.model.BatterySnapshot
import dev.involvex.kiln.core.model.NetstatsSnapshot
import dev.involvex.kiln.core.model.TopSnapshot
import dev.involvex.kiln.feature.permissions.R
import java.util.Locale
import kotlin.math.abs

@Composable
fun DiagnosticsRoute(onBack: () -> Unit, onOpenPrivilegeSetup: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel = kilnHiltViewModel<DiagnosticsViewModel>()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DiagnosticsScreen(
        state = state,
        onBack = onBack,
        onOpenPrivilegeSetup = onOpenPrivilegeSetup,
        onRefresh = viewModel::refresh,
        modifier = modifier,
    )
}

@Composable
fun DiagnosticsScreen(
    state: DiagnosticsUiState,
    onBack: () -> Unit,
    onOpenPrivilegeSetup: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    KilnScaffold(title = stringResource(R.string.diagnostics_title), modifier = modifier) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.permissions_back))
            }
            Text(
                stringResource(R.string.diagnostics_explain),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!state.canRun) {
                Text(
                    stringResource(R.string.diagnostics_need_shizuku),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedButton(onClick = onOpenPrivilegeSetup, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.diagnostics_open_setup))
                }
            } else {
                OutlinedButton(
                    onClick = onRefresh,
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.diagnostics_refresh))
                }
            }
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            if (state.canRun) {
                BatteryCard(state.battery, state.batteryError)
                NetstatsCard(state.netstats, state.netstatsError)
                TopCard(state.top, state.topError)
            }
        }
    }
}

@Composable
private fun BatteryCard(battery: BatterySnapshot?, error: String?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.diagnostics_battery_title), style = MaterialTheme.typography.titleMedium)
            when {
                error != null -> {
                    Text(
                        stringResource(R.string.diagnostics_section_error, error),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                battery == null -> {
                    Text(
                        stringResource(R.string.diagnostics_section_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                else -> {
                    Text(
                        stringResource(R.string.diagnostics_battery_level, battery.levelPercent),
                        style = MaterialTheme.typography.displaySmall,
                    )
                    LinearProgressIndicator(
                        progress = { battery.levelPercent / 100f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        stringResource(
                            R.string.diagnostics_battery_status_line,
                            batteryStatusLabel(battery.status),
                            batteryPowerLabel(battery.powerSource),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        stringResource(R.string.diagnostics_battery_health, batteryHealthLabel(battery.health)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    battery.temperatureCentiC?.let { tenths ->
                        Text(
                            stringResource(
                                R.string.diagnostics_battery_temp,
                                String.format(Locale.US, "%.1f", tenths / 10.0),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    battery.voltageMv?.let { mv ->
                        Text(
                            stringResource(
                                R.string.diagnostics_battery_voltage,
                                String.format(Locale.US, "%.2f", mv / 1000.0),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    battery.currentUa?.let { ua ->
                        Text(
                            stringResource(
                                R.string.diagnostics_battery_current,
                                String.format(Locale.US, "%.0f", ua / 1000.0),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    battery.technology?.let { tech ->
                        Text(
                            stringResource(R.string.diagnostics_battery_tech, tech),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NetstatsCard(netstats: NetstatsSnapshot?, error: String?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.diagnostics_net_title), style = MaterialTheme.typography.titleMedium)
            when {
                error != null -> {
                    Text(
                        stringResource(R.string.diagnostics_section_error, error),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                netstats == null -> {
                    Text(
                        stringResource(R.string.diagnostics_section_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                else -> {
                    Text(
                        stringResource(
                            R.string.diagnostics_net_wifi,
                            formatBytes(netstats.wifiRxBytes),
                            formatBytes(netstats.wifiTxBytes),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        stringResource(
                            R.string.diagnostics_net_mobile,
                            formatBytes(netstats.mobileRxBytes),
                            formatBytes(netstats.mobileTxBytes),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (netstats.topUids.isEmpty()) {
                        Text(
                            stringResource(R.string.diagnostics_net_no_uids),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(
                            stringResource(R.string.diagnostics_net_top),
                            style = MaterialTheme.typography.labelLarge,
                        )
                        netstats.topUids.forEach { row ->
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    row.label,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    formatBytes(row.totalBytes),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopCard(top: TopSnapshot?, error: String?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.diagnostics_cpu_title), style = MaterialTheme.typography.titleMedium)
            when {
                error != null -> {
                    Text(
                        stringResource(R.string.diagnostics_section_error, error),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                top == null -> {
                    Text(
                        stringResource(R.string.diagnostics_section_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                else -> {
                    if (top.userPercent != null || top.systemPercent != null || top.idlePercent != null) {
                        Text(
                            stringResource(
                                R.string.diagnostics_cpu_summary,
                                formatPct(top.userPercent),
                                formatPct(top.systemPercent),
                                formatPct(top.idlePercent),
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    if (top.rows.isEmpty()) {
                        Text(
                            stringResource(R.string.diagnostics_cpu_no_rows),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        top.rows.forEach { row ->
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    stringResource(
                                        R.string.diagnostics_cpu_row_pct,
                                        String.format(Locale.US, "%.1f", row.cpuPercent),
                                    ),
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(end = 4.dp),
                                )
                                Text(
                                    row.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun batteryStatusLabel(status: BatteryChargeStatus): String = stringResource(
    when (status) {
        BatteryChargeStatus.CHARGING -> R.string.diagnostics_battery_status_charging
        BatteryChargeStatus.DISCHARGING -> R.string.diagnostics_battery_status_discharging
        BatteryChargeStatus.NOT_CHARGING -> R.string.diagnostics_battery_status_not_charging
        BatteryChargeStatus.FULL -> R.string.diagnostics_battery_status_full
        BatteryChargeStatus.UNKNOWN -> R.string.diagnostics_battery_status_unknown
    },
)

@Composable
private fun batteryPowerLabel(source: BatteryPowerSource): String = stringResource(
    when (source) {
        BatteryPowerSource.AC -> R.string.diagnostics_battery_power_ac
        BatteryPowerSource.USB -> R.string.diagnostics_battery_power_usb
        BatteryPowerSource.WIRELESS -> R.string.diagnostics_battery_power_wireless
        BatteryPowerSource.DOCK -> R.string.diagnostics_battery_power_dock
        BatteryPowerSource.NONE -> R.string.diagnostics_battery_power_none
    },
)

@Composable
private fun batteryHealthLabel(health: BatteryHealth): String = stringResource(
    when (health) {
        BatteryHealth.GOOD -> R.string.diagnostics_battery_health_good
        BatteryHealth.OVERHEAT -> R.string.diagnostics_battery_health_overheat
        BatteryHealth.DEAD -> R.string.diagnostics_battery_health_dead
        BatteryHealth.OVER_VOLTAGE -> R.string.diagnostics_battery_health_over_voltage
        BatteryHealth.FAILURE -> R.string.diagnostics_battery_health_failure
        BatteryHealth.COLD -> R.string.diagnostics_battery_health_cold
        BatteryHealth.UNKNOWN -> R.string.diagnostics_battery_health_unknown
    },
)

private fun formatPct(value: Float?): String = if (value == null) "—" else String.format(Locale.US, "%.0f", value)

private fun formatBytes(bytes: Long): String {
    val abs = abs(bytes.toDouble())
    return when {
        abs >= 1_073_741_824 -> String.format(Locale.US, "%.1f GB", bytes / 1_073_741_824.0)
        abs >= 1_048_576 -> String.format(Locale.US, "%.1f MB", bytes / 1_048_576.0)
        abs >= 1024 -> String.format(Locale.US, "%.0f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}

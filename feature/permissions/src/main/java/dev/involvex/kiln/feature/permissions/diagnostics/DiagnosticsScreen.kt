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
import dev.involvex.kiln.core.model.CpuinfoSnapshot
import dev.involvex.kiln.core.model.NetstatsSnapshot
import dev.involvex.kiln.core.model.ThermalSnapshot
import dev.involvex.kiln.core.model.ThermalStatusLevel
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
                CpuinfoCard(state.cpuinfo, state.cpuinfoError)
                ThermalCard(state.thermal, state.thermalError)
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
private fun CpuinfoCard(cpuinfo: CpuinfoSnapshot?, error: String?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.diagnostics_cpuinfo_title), style = MaterialTheme.typography.titleMedium)
            when {
                error != null -> {
                    Text(
                        stringResource(R.string.diagnostics_section_error, error),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                cpuinfo == null -> {
                    Text(
                        stringResource(R.string.diagnostics_section_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                else -> {
                    if (cpuinfo.load1 != null || cpuinfo.load5 != null || cpuinfo.load15 != null) {
                        Text(
                            stringResource(
                                R.string.diagnostics_cpuinfo_load,
                                formatLoad(cpuinfo.load1),
                                formatLoad(cpuinfo.load5),
                                formatLoad(cpuinfo.load15),
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    if (cpuinfo.rows.isEmpty()) {
                        Text(
                            stringResource(R.string.diagnostics_cpuinfo_no_rows),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        cpuinfo.rows.forEach { row ->
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
                                )
                                Text(
                                    row.packageName,
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
private fun ThermalCard(thermal: ThermalSnapshot?, error: String?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.diagnostics_thermal_title), style = MaterialTheme.typography.titleMedium)
            when {
                error != null -> {
                    Text(
                        stringResource(R.string.diagnostics_section_error, error),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                thermal == null -> {
                    Text(
                        stringResource(R.string.diagnostics_section_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                else -> {
                    Text(
                        stringResource(R.string.diagnostics_thermal_status, thermalStatusLabel(thermal.status)),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (thermal.zones.isEmpty()) {
                        Text(
                            stringResource(R.string.diagnostics_thermal_no_zones),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        thermal.zones.forEach { zone ->
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    zone.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    stringResource(
                                        R.string.diagnostics_thermal_zone_temp,
                                        String.format(Locale.US, "%.1f", zone.celsius),
                                    ),
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
private fun thermalStatusLabel(status: ThermalStatusLevel): String = stringResource(
    when (status) {
        ThermalStatusLevel.NONE -> R.string.diagnostics_thermal_status_none
        ThermalStatusLevel.LIGHT -> R.string.diagnostics_thermal_status_light
        ThermalStatusLevel.MODERATE -> R.string.diagnostics_thermal_status_moderate
        ThermalStatusLevel.SEVERE -> R.string.diagnostics_thermal_status_severe
        ThermalStatusLevel.CRITICAL -> R.string.diagnostics_thermal_status_critical
        ThermalStatusLevel.EMERGENCY -> R.string.diagnostics_thermal_status_emergency
        ThermalStatusLevel.SHUTDOWN -> R.string.diagnostics_thermal_status_shutdown
        ThermalStatusLevel.UNKNOWN -> R.string.diagnostics_thermal_status_unknown
    },
)

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

private fun formatLoad(value: Float?): String = if (value == null) "—" else String.format(Locale.US, "%.2f", value)

private fun formatBytes(bytes: Long): String {
    val abs = abs(bytes.toDouble())
    return when {
        abs >= 1_073_741_824 -> String.format(Locale.US, "%.1f GB", bytes / 1_073_741_824.0)
        abs >= 1_048_576 -> String.format(Locale.US, "%.1f MB", bytes / 1_048_576.0)
        abs >= 1024 -> String.format(Locale.US, "%.0f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}

package dev.involvex.kiln.feature.permissions.diagnostics

import dev.involvex.kiln.core.model.BatterySnapshot
import dev.involvex.kiln.core.model.CpuinfoSnapshot
import dev.involvex.kiln.core.model.NetstatsSnapshot
import dev.involvex.kiln.core.model.ThermalSnapshot
import dev.involvex.kiln.core.model.TopSnapshot

data class DiagnosticsUiState(
    val canRun: Boolean = false,
    val isLoading: Boolean = false,
    val battery: BatterySnapshot? = null,
    val netstats: NetstatsSnapshot? = null,
    val top: TopSnapshot? = null,
    val cpuinfo: CpuinfoSnapshot? = null,
    val thermal: ThermalSnapshot? = null,
    val batteryError: String? = null,
    val netstatsError: String? = null,
    val topError: String? = null,
    val cpuinfoError: String? = null,
    val thermalError: String? = null,
)

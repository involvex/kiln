package dev.involvex.kiln.core.model

/** Charge status from `dumpsys battery` status int. */
enum class BatteryChargeStatus {
    UNKNOWN,
    CHARGING,
    DISCHARGING,
    NOT_CHARGING,
    FULL,
}

/** Battery health from `dumpsys battery` health int. */
enum class BatteryHealth {
    UNKNOWN,
    GOOD,
    OVERHEAT,
    DEAD,
    OVER_VOLTAGE,
    FAILURE,
    COLD,
}

/** Plugged power source from `dumpsys battery` powered flags. */
enum class BatteryPowerSource {
    NONE,
    AC,
    USB,
    WIRELESS,
    DOCK,
}

/**
 * Compact battery snapshot for Device diagnostics (M60).
 * [temperatureCentiC] is tenths of a degree from dumpsys (`temperature: 312` → 31.2 °C).
 */
data class BatterySnapshot(
    val levelPercent: Int,
    val status: BatteryChargeStatus,
    val health: BatteryHealth,
    val powerSource: BatteryPowerSource,
    val technology: String?,
    val temperatureCentiC: Int?,
    val voltageMv: Int?,
    val currentUa: Int?,
)

/** One UID row from netstats (bytes in/out). Labels resolved by the executor. */
data class NetstatsUidRow(val uid: Int, val label: String, val rxBytes: Long, val txBytes: Long) {
    val totalBytes: Long get() = rxBytes + txBytes
}

/** Compact network usage snapshot (privacy-stripped; no SSID / subscriberId). */
data class NetstatsSnapshot(
    val wifiRxBytes: Long,
    val wifiTxBytes: Long,
    val mobileRxBytes: Long,
    val mobileTxBytes: Long,
    val topUids: List<NetstatsUidRow>,
)

/** One process row from `top -n 1`. */
data class TopProcessRow(val pid: Int, val cpuPercent: Float, val name: String)

/** Compact CPU / top snapshot. */
data class TopSnapshot(val userPercent: Float?, val systemPercent: Float?, val idlePercent: Float?, val rows: List<TopProcessRow>)

/** One package row from `dumpsys cpuinfo`. */
data class CpuinfoPackageRow(val packageName: String, val cpuPercent: Float)

/**
 * Compact `dumpsys cpuinfo` snapshot (M64).
 * [load1]/[load5]/[load15] are optional 1/5/15-minute load averages.
 */
data class CpuinfoSnapshot(val load1: Float?, val load5: Float?, val load15: Float?, val rows: List<CpuinfoPackageRow>)

/** Skin / HAL thermal severity from `dumpsys thermalservice` Thermal Status. */
enum class ThermalStatusLevel {
    NONE,
    LIGHT,
    MODERATE,
    SEVERE,
    CRITICAL,
    EMERGENCY,
    SHUTDOWN,
    UNKNOWN,
}

/** One temperature sensor from thermalservice. */
data class ThermalZoneRow(val name: String, val celsius: Float, val type: Int?)

/** Compact `dumpsys thermalservice` snapshot (M64). */
data class ThermalSnapshot(val status: ThermalStatusLevel, val zones: List<ThermalZoneRow>)

object DiagnosticsGate {
    fun canRun(capabilities: Set<PrivilegeCapability>): Boolean = PrivilegeCapability.RUN_SHELL_DIAGNOSTIC in capabilities
}

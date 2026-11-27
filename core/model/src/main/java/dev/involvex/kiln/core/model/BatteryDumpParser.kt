package dev.involvex.kiln.core.model

/** Pure parser for `dumpsys battery` stdout. */
object BatteryDumpParser {
    private const val STATUS_CHARGING = 2
    private const val STATUS_DISCHARGING = 3
    private const val STATUS_NOT_CHARGING = 4
    private const val STATUS_FULL = 5
    private const val HEALTH_GOOD = 2
    private const val HEALTH_OVERHEAT = 3
    private const val HEALTH_DEAD = 4
    private const val HEALTH_OVER_VOLTAGE = 5
    private const val HEALTH_FAILURE = 6
    private const val HEALTH_COLD = 7

    private val KEY_VALUE = Regex("""^\s*([\w\s]+?):\s*(.+)\s*$""")

    fun parse(stdout: String): BatterySnapshot? {
        if (stdout.isBlank() || stdout.startsWith("ERR:")) return null
        val map = LinkedHashMap<String, String>()
        for (line in stdout.lineSequence()) {
            val match = KEY_VALUE.find(line.trim()) ?: continue
            val key = match.groupValues[1].trim().lowercase()
            val value = match.groupValues[2].trim()
            if (key.isNotEmpty()) map[key] = value
        }
        if (map.isEmpty()) return null
        val level = map["level"]?.toIntOrNull() ?: return null
        return BatterySnapshot(
            levelPercent = level.coerceIn(0, 100),
            status = parseStatus(map["status"]?.toIntOrNull()),
            health = parseHealth(map["health"]?.toIntOrNull()),
            powerSource = parsePowerSource(map),
            technology = map["technology"]?.takeIf { it.isNotBlank() && !it.equals("unknown", ignoreCase = true) },
            temperatureCentiC = map["temperature"]?.toIntOrNull(),
            voltageMv = map["voltage"]?.toIntOrNull(),
            currentUa = map["current now"]?.toIntOrNull()
                ?: map["currentnow"]?.toIntOrNull(),
        )
    }

    private fun parseStatus(code: Int?): BatteryChargeStatus = when (code) {
        STATUS_CHARGING -> BatteryChargeStatus.CHARGING
        STATUS_DISCHARGING -> BatteryChargeStatus.DISCHARGING
        STATUS_NOT_CHARGING -> BatteryChargeStatus.NOT_CHARGING
        STATUS_FULL -> BatteryChargeStatus.FULL
        else -> BatteryChargeStatus.UNKNOWN
    }

    private fun parseHealth(code: Int?): BatteryHealth = when (code) {
        HEALTH_GOOD -> BatteryHealth.GOOD
        HEALTH_OVERHEAT -> BatteryHealth.OVERHEAT
        HEALTH_DEAD -> BatteryHealth.DEAD
        HEALTH_OVER_VOLTAGE -> BatteryHealth.OVER_VOLTAGE
        HEALTH_FAILURE -> BatteryHealth.FAILURE
        HEALTH_COLD -> BatteryHealth.COLD
        else -> BatteryHealth.UNKNOWN
    }

    private fun parsePowerSource(map: Map<String, String>): BatteryPowerSource = when {
        map["ac powered"].equals("true", ignoreCase = true) -> BatteryPowerSource.AC
        map["wireless powered"].equals("true", ignoreCase = true) -> BatteryPowerSource.WIRELESS
        map["usb powered"].equals("true", ignoreCase = true) -> BatteryPowerSource.USB
        map["dock powered"].equals("true", ignoreCase = true) -> BatteryPowerSource.DOCK
        else -> BatteryPowerSource.NONE
    }
}

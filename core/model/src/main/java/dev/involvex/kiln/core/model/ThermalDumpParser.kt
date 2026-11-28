package dev.involvex.kiln.core.model

/**
 * Pure parser for `dumpsys thermalservice`.
 * Prefers HAL current temperatures; falls back to Cached; skips sentinel temps.
 */
@Suppress("CyclomaticComplexMethod", "MagicNumber", "ReturnCount", "LoopWithTooManyJumpStatements")
object ThermalDumpParser {
    private const val DEFAULT_LIMIT = 12
    private const val STATUS_SCAN_LINES = 40
    private const val SENTINEL_CELSIUS = -50f
    private const val STATUS_NONE = 0
    private const val STATUS_LIGHT = 1
    private const val STATUS_MODERATE = 2
    private const val STATUS_SEVERE = 3
    private const val STATUS_CRITICAL = 4
    private const val STATUS_EMERGENCY = 5
    private const val STATUS_SHUTDOWN = 6
    private val STATUS = Regex("""Thermal\s+Status:\s*(-?\d+)""", RegexOption.IGNORE_CASE)
    private val TEMP = Regex(
        """Temperature\{[^}]*mValue\s*=\s*([-\d.]+)[^}]*mType\s*=\s*(-?\d+)[^}]*mName\s*=\s*([^,}]+)""",
        RegexOption.IGNORE_CASE,
    )
    private val TEMP_ALT = Regex(
        """mName\s*=\s*([^,}]+)[^}]*mValue\s*=\s*([-\d.]+)[^}]*mType\s*=\s*(-?\d+)""",
        RegexOption.IGNORE_CASE,
    )

    fun parse(stdout: String, limit: Int = DEFAULT_LIMIT): ThermalSnapshot? {
        if (stdout.isBlank() || stdout.startsWith("ERR:")) return null
        val lines = stdout.lineSequence().map { it.trimEnd() }.toList()
        if (lines.isEmpty()) return null

        var status = ThermalStatusLevel.UNKNOWN
        for (line in lines.take(STATUS_SCAN_LINES)) {
            STATUS.find(line)?.let { m ->
                status = mapStatus(m.groupValues[1].toIntOrNull())
            }
        }

        val preferred = preferredTemps(lines)
        val zones = preferred
            .sortedByDescending { it.celsius }
            .take(limit.coerceAtLeast(0))
        if (status == ThermalStatusLevel.UNKNOWN && zones.isEmpty()) return null
        return ThermalSnapshot(status = status, zones = zones)
    }

    private fun preferredTemps(lines: List<String>): List<ThermalZoneRow> {
        val halStart = lines.indexOfFirst { it.contains("Current temperatures from HAL", ignoreCase = true) }
        val cachedStart = lines.indexOfFirst { it.contains("Cached temperatures", ignoreCase = true) }
        return when {
            halStart >= 0 -> extractTemps(lines, halStart + 1, nextSection(lines, halStart + 1))
            cachedStart >= 0 -> extractTemps(lines, cachedStart + 1, nextSection(lines, cachedStart + 1))
            else -> extractTemps(lines, 0, lines.size)
        }
    }

    private fun nextSection(lines: List<String>, from: Int): Int {
        for (i in from until lines.size) {
            val t = lines[i].trim()
            if (t.isEmpty()) continue
            if (t.endsWith(":") && !t.contains("Temperature", ignoreCase = true)) return i
        }
        return lines.size
    }

    private fun extractTemps(lines: List<String>, from: Int, until: Int): List<ThermalZoneRow> {
        val out = LinkedHashMap<String, ThermalZoneRow>()
        for (i in from until until.coerceAtMost(lines.size)) {
            val row = parseTempLine(lines[i].trim()) ?: continue
            if (row.celsius <= SENTINEL_CELSIUS || row.celsius.isNaN()) continue
            out.putIfAbsent(row.name, row)
        }
        return out.values.toList()
    }

    private fun parseTempLine(line: String): ThermalZoneRow? {
        if (line.isEmpty()) return null
        TEMP.find(line)?.let { m ->
            val value = m.groupValues[1].toFloatOrNull() ?: return null
            val type = m.groupValues[2].toIntOrNull()
            val name = m.groupValues[3].trim().trim('"').ifBlank { return null }
            return ThermalZoneRow(name = name, celsius = value, type = type)
        }
        TEMP_ALT.find(line)?.let { m ->
            val name = m.groupValues[1].trim().trim('"').ifBlank { return null }
            val value = m.groupValues[2].toFloatOrNull() ?: return null
            val type = m.groupValues[3].toIntOrNull()
            return ThermalZoneRow(name = name, celsius = value, type = type)
        }
        return null
    }

    private fun mapStatus(code: Int?): ThermalStatusLevel = when (code) {
        STATUS_NONE -> ThermalStatusLevel.NONE
        STATUS_LIGHT -> ThermalStatusLevel.LIGHT
        STATUS_MODERATE -> ThermalStatusLevel.MODERATE
        STATUS_SEVERE -> ThermalStatusLevel.SEVERE
        STATUS_CRITICAL -> ThermalStatusLevel.CRITICAL
        STATUS_EMERGENCY -> ThermalStatusLevel.EMERGENCY
        STATUS_SHUTDOWN -> ThermalStatusLevel.SHUTDOWN
        else -> ThermalStatusLevel.UNKNOWN
    }
}

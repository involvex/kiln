package dev.involvex.kiln.core.model

data class ProcessStat(val packageName: String, val label: String, val pssKb: Long, val cpuPercent: Float?, val uid: Int?, val pid: Int?)

object ProcessStatsGate {
    fun canList(capabilities: Set<PrivilegeCapability>): Boolean = PrivilegeCapability.RUN_SHELL_DIAGNOSTIC in capabilities

    fun canForceStop(capabilities: Set<PrivilegeCapability>): Boolean = PrivilegeCapability.FORCE_STOP in capabilities ||
        PrivilegeCapability.ACCESS_ANDROID_DATA in capabilities
}

/** Cache widget: clear only after confirm was requested. */
object CacheClearConfirmGate {
    fun mayExecute(confirmPending: Boolean): Boolean = confirmPending
}

/** Processes widget: force-stop only when pending package matches and is not Kiln. */
object ForceStopConfirmGate {
    fun mayExecute(pendingPackage: String?, targetPackage: String, kilnPackageName: String): Boolean {
        if (pendingPackage.isNullOrBlank()) return false
        if (pendingPackage != targetPackage) return false
        if (targetPackage == kilnPackageName) return false
        return ProcessStatsParser.looksLikePackage(targetPackage)
    }
}

/**
 * Parses `dumpsys meminfo` "Total PSS by process:" section and optional `dumpsys cpuinfo` lines.
 */
object ProcessStatsParser {
    private const val PACKAGE_NAME_MAX = 255
    private const val PACKAGE_NAME_MIN = 3
    private const val DEFAULT_LIMIT = 6

    private val PSS_LINE = Regex(
        """^\s*([\d,]+)\s*kB:\s+(\S+)(?:\s+\(pid\s+(\d+)(?:\s*/\s*activities)?\))?""",
        RegexOption.IGNORE_CASE,
    )
    private val CPU_LINE = Regex(
        """^\s*([\d.]+)%\s+\d+/([a-zA-Z][\w.]*(?:\.[a-zA-Z][\w.]*)+):""",
    )

    fun parseMeminfo(stdout: String): List<ProcessStat> {
        val lines = stdout.lineSequence().toList()
        val start = lines.indexOfFirst { it.contains("Total PSS by process", ignoreCase = true) }
        if (start < 0) return emptyList()
        return lines
            .asSequence()
            .drop(start + 1)
            .takeWhile { line -> line.isNotBlank() && !line.contains("Total PSS by", ignoreCase = true) }
            .mapNotNull { parsePssLine(it) }
            .toList()
    }

    fun parseCpuinfo(stdout: String): Map<String, Float> = stdout.lineSequence()
        .mapNotNull { parseCpuLine(it) }
        .toMap(LinkedHashMap())

    fun merge(
        mem: List<ProcessStat>,
        cpuByPackage: Map<String, Float>,
        kilnPackageName: String,
        limit: Int = DEFAULT_LIMIT,
    ): List<ProcessStat> = mem
        .asSequence()
        .filter { it.packageName != kilnPackageName }
        .filter { looksLikePackage(it.packageName) }
        .map { row ->
            row.copy(cpuPercent = cpuByPackage[row.packageName])
        }
        .sortedByDescending { it.pssKb }
        .take(limit.coerceAtLeast(0))
        .toList()

    fun looksLikePackage(value: String): Boolean = value.length in PACKAGE_NAME_MIN..PACKAGE_NAME_MAX && value.contains('.') &&
        value.all { it.isLetterOrDigit() || it == '.' || it == '_' }

    private fun parsePssLine(line: String): ProcessStat? {
        val match = PSS_LINE.find(line) ?: return null
        val pss = match.groupValues[1].replace(",", "").toLongOrNull() ?: return null
        val name = match.groupValues[2]
        if (!looksLikePackage(name)) return null
        val pid = match.groupValues.getOrNull(3)?.toIntOrNull()
        return ProcessStat(
            packageName = name,
            label = name.substringAfterLast('.'),
            pssKb = pss,
            cpuPercent = null,
            uid = null,
            pid = pid,
        )
    }

    private fun parseCpuLine(line: String): Pair<String, Float>? {
        val match = CPU_LINE.find(line) ?: return null
        val pct = match.groupValues[1].toFloatOrNull() ?: return null
        return match.groupValues[2] to pct
    }
}

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
 * Parses `dumpsys meminfo` process sections and optional `dumpsys cpuinfo` lines.
 *
 * HyperOS / recent Android often print `Total RSS by process` / `N,NNNK:` instead of
 * classic `Total PSS by process` / `N kB:`.
 */
object ProcessStatsParser {
    private const val PACKAGE_NAME_MAX = 255
    private const val PACKAGE_NAME_MIN = 3
    private const val DEFAULT_LIMIT = 6

    /** Matches `1,251,781K:` and `120,000 kB:` (HyperOS + AOSP). */
    private val MEM_LINE = Regex(
        """^\s*([\d,]+)\s*(?:kB|K):\s+(\S+)(?:\s+\(pid\s+(\d+)(?:\s*/\s*activities)?\))?""",
        RegexOption.IGNORE_CASE,
    )
    private val CPU_LINE = Regex(
        """^\s*([\d.]+)%\s+\d+/([a-zA-Z][\w.]*(?:\.[a-zA-Z][\w.]*)+):""",
    )

    fun parseMeminfo(stdout: String): List<ProcessStat> {
        val lines = stdout.lineSequence().toList()
        val start = sectionStart(lines) ?: return emptyList()
        return lines
            .asSequence()
            .drop(start + 1)
            .takeWhile { line -> line.isNotBlank() && !isSectionHeader(line) }
            .mapNotNull { parseMemLine(it) }
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
        // Collapse multi-process packages (e.g. :background) keeping the largest entry.
        .distinctBy { it.packageName }
        .take(limit.coerceAtLeast(0))
        .toList()

    fun looksLikePackage(value: String): Boolean = value.length in PACKAGE_NAME_MIN..PACKAGE_NAME_MAX && value.contains('.') &&
        value.all { it.isLetterOrDigit() || it == '.' || it == '_' }

    /** `com.foo:service` → `com.foo` for force-stop / package lookup. */
    fun basePackageName(processName: String): String = processName.substringBefore(':')

    private fun sectionStart(lines: List<String>): Int? {
        val pss = lines.indexOfFirst { it.contains("Total PSS by process", ignoreCase = true) }
        if (pss >= 0) return pss
        val rss = lines.indexOfFirst { it.contains("Total RSS by process", ignoreCase = true) }
        return rss.takeIf { it >= 0 }
    }

    private fun isSectionHeader(line: String): Boolean = line.contains("Total PSS by", ignoreCase = true) ||
        line.contains("Total RSS by", ignoreCase = true)

    private fun parseMemLine(line: String): ProcessStat? {
        val match = MEM_LINE.find(line) ?: return null
        val pss = match.groupValues[1].replace(",", "").toLongOrNull() ?: return null
        val processName = match.groupValues[2]
        val pkg = basePackageName(processName)
        if (!looksLikePackage(pkg)) return null
        val pid = match.groupValues.getOrNull(3)?.toIntOrNull()
        return ProcessStat(
            packageName = pkg,
            label = pkg.substringAfterLast('.'),
            pssKb = pss,
            cpuPercent = null,
            uid = null,
            pid = pid,
        )
    }

    private fun parseCpuLine(line: String): Pair<String, Float>? {
        val match = CPU_LINE.find(line) ?: return null
        val pct = match.groupValues[1].toFloatOrNull() ?: return null
        return basePackageName(match.groupValues[2]) to pct
    }
}

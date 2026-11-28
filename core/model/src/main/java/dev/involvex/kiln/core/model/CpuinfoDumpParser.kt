package dev.involvex.kiln.core.model

/** Pure parser for `dumpsys cpuinfo` (load line + package % rows). */
@Suppress("ComplexCondition", "MagicNumber", "ReturnCount")
object CpuinfoDumpParser {
    private const val DEFAULT_LIMIT = 8
    private val LOAD = Regex(
        """Load:\s*([\d.]+)\s*/\s*([\d.]+)\s*/\s*([\d.]+)""",
        RegexOption.IGNORE_CASE,
    )

    fun parse(stdout: String, limit: Int = DEFAULT_LIMIT): CpuinfoSnapshot? {
        if (stdout.isBlank() || stdout.startsWith("ERR:")) return null
        val load = parseLoad(stdout)
        val byPkg = ProcessStatsParser.parseCpuinfo(stdout)
        val rows = byPkg.entries
            .sortedByDescending { it.value }
            .take(limit.coerceAtLeast(0))
            .map { (pkg, pct) -> CpuinfoPackageRow(packageName = pkg, cpuPercent = pct) }
        if (load == null && rows.isEmpty()) return null
        return CpuinfoSnapshot(
            load1 = load?.first,
            load5 = load?.second,
            load15 = load?.third,
            rows = rows,
        )
    }

    private fun parseLoad(stdout: String): Triple<Float?, Float?, Float?>? {
        for (line in stdout.lineSequence()) {
            val match = LOAD.find(line.trim()) ?: continue
            return Triple(
                match.groupValues[1].toFloatOrNull(),
                match.groupValues[2].toFloatOrNull(),
                match.groupValues[3].toFloatOrNull(),
            )
        }
        return null
    }
}

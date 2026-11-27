package dev.involvex.kiln.core.model

/** Pure parser for toybox `top -n 1` stdout. */
@Suppress("CyclomaticComplexMethod", "ComplexCondition", "MagicNumber", "ReturnCount")
object TopDumpParser {
    private const val DEFAULT_LIMIT = 8
    private const val SUMMARY_SCAN_LINES = 12
    private val SUMMARY = Regex(
        """User\s+([\d.]+)%.*?System\s+([\d.]+)%.*?Idle\s+([\d.]+)%""",
        RegexOption.IGNORE_CASE,
    )
    private val SUMMARY_ALT = Regex(
        """([\d.]+)%\s*user.*?([\d.]+)%\s*sys(?:tem)?.*?([\d.]+)%\s*idle""",
        RegexOption.IGNORE_CASE,
    )
    private val KERNEL_BRACKET = Regex("""\[[^\]]+]""")

    fun parse(stdout: String, limit: Int = DEFAULT_LIMIT): TopSnapshot? {
        if (stdout.isBlank() || stdout.startsWith("ERR:")) return null
        val lines = stdout.lineSequence().map { it.trimEnd() }.filter { it.isNotBlank() }.toList()
        if (lines.isEmpty()) return null

        val (user, system, idle) = parseSummary(lines)
        val headerIdx = lines.indexOfFirst { line ->
            val lower = line.lowercase()
            lower.contains("pid") && (lower.contains("%cpu") || lower.contains("cpu"))
        }
        val dataLines = if (headerIdx >= 0) {
            lines.drop(headerIdx + 1)
        } else {
            lines.dropWhile { isHeaderish(it) }
        }
        val cols = if (headerIdx >= 0) locateColumns(lines[headerIdx]) else null
        val rows = dataLines
            .mapNotNull { line -> parseRow(line, cols) }
            .filter { row -> !isKernelThread(row.name) }
            .sortedByDescending { it.cpuPercent }
            .distinctBy { it.pid }
            .take(limit.coerceAtLeast(0))

        if (user == null && system == null && idle == null && rows.isEmpty()) return null
        return TopSnapshot(
            userPercent = user,
            systemPercent = system,
            idlePercent = idle,
            rows = rows,
        )
    }

    private fun isHeaderish(line: String): Boolean = line.startsWith("User", ignoreCase = true) ||
        line.startsWith("Tasks", ignoreCase = true) ||
        (line.contains("%cpu", ignoreCase = true) && line.contains("pid", ignoreCase = true))

    private fun parseSummary(lines: List<String>): Triple<Float?, Float?, Float?> {
        var user: Float? = null
        var system: Float? = null
        var idle: Float? = null
        for (line in lines.take(SUMMARY_SCAN_LINES)) {
            SUMMARY.find(line)?.let { m ->
                user = m.groupValues[1].toFloatOrNull()
                system = m.groupValues[2].toFloatOrNull()
                idle = m.groupValues[3].toFloatOrNull()
            }
            if (user == null) {
                SUMMARY_ALT.find(line)?.let { m ->
                    user = m.groupValues[1].toFloatOrNull()
                    system = m.groupValues[2].toFloatOrNull()
                    idle = m.groupValues[3].toFloatOrNull()
                }
            }
        }
        return Triple(user, system, idle)
    }

    private data class TopColumns(val cpu: Int, val name: Int)

    private fun locateColumns(header: String): TopColumns? {
        val tokens = header.trim().split(Regex("""\s+"""))
        val cpu = tokens.indexOfFirst {
            it.contains("%cpu", ignoreCase = true) || it.equals("cpu", ignoreCase = true) ||
                it.equals("cpu%", ignoreCase = true)
        }.takeIf { it >= 0 } ?: return null
        val name = tokens.indexOfFirst {
            it.equals("COMMAND", ignoreCase = true) || it.equals("NAME", ignoreCase = true) ||
                it.equals("ARGS", ignoreCase = true)
        }.takeIf { it >= 0 } ?: tokens.lastIndex.coerceAtLeast(cpu + 1)
        return TopColumns(cpu = cpu, name = name)
    }

    private fun parseRow(line: String, cols: TopColumns?): TopProcessRow? {
        val tokens = line.trim().split(Regex("""\s+"""))
        if (tokens.size < 3) return null
        val pid = tokens[0].toIntOrNull() ?: return null
        val cpu = if (cols != null && cols.cpu < tokens.size) {
            tokens[cols.cpu].toFloatOrNull()
        } else {
            tokens.drop(1).firstNotNullOfOrNull { it.toFloatOrNull() }
        } ?: return null
        val name = if (cols != null && cols.name < tokens.size) {
            tokens.drop(cols.name).joinToString(" ")
        } else {
            tokens.last()
        }.ifBlank { tokens.last() }
        if (name.isBlank()) return null
        return TopProcessRow(pid = pid, cpuPercent = cpu, name = name.trim())
    }

    private fun isKernelThread(name: String): Boolean {
        val t = name.trim()
        if (t.startsWith('[') && t.endsWith(']')) return true
        return KERNEL_BRACKET.containsMatchIn(t) && !t.contains('.')
    }
}

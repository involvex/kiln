package dev.involvex.kiln.core.privilege.shell

/**
 * Pure helpers for an early-exit [dumpsys meminfo] read.
 * HyperOS prints `Total RSS by process` near the top; waiting for the full dump ANRs widgets.
 */
object MeminfoSnapshot {
    fun isProcessSectionStart(line: String): Boolean = line.contains("Total RSS by process", ignoreCase = true) ||
        line.contains("Total PSS by process", ignoreCase = true)

    /** Next `Total …` header (not the process list itself) ends the first process section. */
    fun isProcessSectionEnd(line: String): Boolean {
        if (!line.contains("Total ", ignoreCase = true)) return false
        return !line.contains("by process", ignoreCase = true)
    }
}

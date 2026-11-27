package dev.involvex.kiln.core.privilege.shell

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

/**
 * Runs allowlisted argv via [ProcessBuilder] (no shell). Used inside the UserService process.
 */
object ShellRunner {
    private const val TIMEOUT_SECONDS = 120L
    private const val MEMINFO_TIMEOUT_MS = 8_000L
    private const val MEMINFO_MAX_PROCESS_LINES = 64
    private const val DIAG_TIMEOUT_MS = 8_000L
    private const val BATTERY_MAX_LINES = 80
    private const val TOP_MAX_LINES = 80

    // UID stats table (`mAppUidStatsMap`) is often past ~600 lines on HyperOS.
    private const val NETSTATS_MAX_LINES = 900
    private const val NETSTATS_MAX_BYTES = 256 * 1024

    fun run(argv: List<String>): String {
        require(argv.isNotEmpty()) { "empty argv" }
        val process = try {
            ProcessBuilder(argv)
                .redirectErrorStream(true)
                .start()
        } catch (e: IOException) {
            return "ERR:start:${e.message}"
        }
        val finished = try {
            process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            process.destroyForcibly()
            return "ERR:interrupted:${e.message}"
        }
        if (!finished) {
            process.destroyForcibly()
            return "ERR:timeout"
        }
        val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
        val code = process.exitValue()
        return if (code == 0) {
            output.ifBlank { "OK" }
        } else {
            "ERR:exit=$code:$output"
        }
    }

    /**
     * Streams [dumpsys meminfo] only until the first RSS/PSS-by-process section ends, then kills
     * the process. Avoids multi-minute full dumps that ANR BroadcastReceivers / freeze the app.
     */
    fun runMeminfoSnapshot(argv: List<String>, timeoutMs: Long = MEMINFO_TIMEOUT_MS): String {
        require(argv.isNotEmpty()) { "empty argv" }
        val process = try {
            ProcessBuilder(argv)
                .redirectErrorStream(true)
                .start()
        } catch (e: IOException) {
            return "ERR:start:${e.message}"
        }
        val deadline = System.currentTimeMillis() + timeoutMs.coerceAtLeast(500L)
        val out = StringBuilder()
        var inSection = false
        var processLines = 0
        try {
            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                while (System.currentTimeMillis() < deadline) {
                    val line = reader.readLine() ?: break
                    out.append(line).append('\n')
                    when {
                        !inSection && MeminfoSnapshot.isProcessSectionStart(line) -> {
                            inSection = true
                        }

                        inSection && MeminfoSnapshot.isProcessSectionEnd(line) -> {
                            break
                        }

                        inSection && line.isNotBlank() -> {
                            processLines++
                            if (processLines >= MEMINFO_MAX_PROCESS_LINES) break
                        }

                        inSection && line.isBlank() && processLines > 0 -> {
                            break
                        }
                    }
                }
            }
        } catch (e: IOException) {
            process.destroyForcibly()
            return "ERR:read:${e.message}"
        } finally {
            process.destroyForcibly()
        }
        val text = out.toString().trim()
        return when {
            text.isBlank() -> "ERR:empty"
            !inSection -> "ERR:no_process_section"
            else -> text
        }
    }

    /** Bounded read for `dumpsys battery` (small, but never hang forever). */
    fun runBatterySnapshot(argv: List<String>, timeoutMs: Long = DIAG_TIMEOUT_MS): String =
        runBoundedLines(argv, timeoutMs, BATTERY_MAX_LINES, maxBytes = 32 * 1024)

    /**
     * Bounded read for `dumpsys netstats` — full dumps can be multi‑MB and take minutes.
     * Stream until line/byte caps, then kill.
     */
    fun runNetstatsSnapshot(argv: List<String>, timeoutMs: Long = DIAG_TIMEOUT_MS): String =
        runBoundedLines(argv, timeoutMs, NETSTATS_MAX_LINES, maxBytes = NETSTATS_MAX_BYTES)

    /** Bounded read for `top -n 1` (toybox may stall on some devices). */
    fun runTopSnapshot(argv: List<String>, timeoutMs: Long = DIAG_TIMEOUT_MS): String =
        runBoundedLines(argv, timeoutMs, TOP_MAX_LINES, maxBytes = 64 * 1024)

    private fun runBoundedLines(argv: List<String>, timeoutMs: Long, maxLines: Int, maxBytes: Int): String {
        require(argv.isNotEmpty()) { "empty argv" }
        val process = try {
            ProcessBuilder(argv)
                .redirectErrorStream(true)
                .start()
        } catch (e: IOException) {
            return "ERR:start:${e.message}"
        }
        val deadline = System.currentTimeMillis() + timeoutMs.coerceAtLeast(500L)
        val out = StringBuilder()
        var lines = 0
        try {
            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                while (System.currentTimeMillis() < deadline) {
                    val line = reader.readLine() ?: break
                    out.append(line).append('\n')
                    lines++
                    if (lines >= maxLines || out.length >= maxBytes) break
                }
            }
        } catch (e: IOException) {
            process.destroyForcibly()
            return "ERR:read:${e.message}"
        } finally {
            process.destroyForcibly()
        }
        val text = out.toString().trim()
        return text.ifBlank { "ERR:empty" }
    }
}

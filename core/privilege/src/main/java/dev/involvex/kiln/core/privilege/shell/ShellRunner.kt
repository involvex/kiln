package dev.involvex.kiln.core.privilege.shell

import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Runs allowlisted argv via [ProcessBuilder] (no shell). Used inside the UserService process.
 */
object ShellRunner {
    private const val TIMEOUT_SECONDS = 120L

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
}

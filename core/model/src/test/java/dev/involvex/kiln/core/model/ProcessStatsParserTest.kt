package dev.involvex.kiln.core.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ProcessStatsParserTest {
    @Test
    fun `parses total pss by process section`() {
        val stdout = """
            Applications Memory Usage:

            Total PSS by process:
                120,000 kB: com.example.heavy (pid 1234 / activities)
                 80,000 kB: com.example.light (pid 99)
                  1,000 kB: system_server (pid 1000)
                50,000 kB: com.android.chrome

            Total PSS by OOM adjustment:
                something else
        """.trimIndent()
        val rows = ProcessStatsParser.parseMeminfo(stdout)
        assertEquals(3, rows.size)
        assertEquals("com.example.heavy", rows[0].packageName)
        assertEquals(120_000L, rows[0].pssKb)
        assertEquals(1234, rows[0].pid)
        assertFalse(rows.any { it.packageName == "system_server" })
    }

    @Test
    fun `parses cpuinfo package percents`() {
        val stdout = """
            12% 1000/com.example.heavy: 10% user + 2% kernel
             3.5% 99/com.example.light: 3% user + 0.5% kernel
        """.trimIndent()
        val map = ProcessStatsParser.parseCpuinfo(stdout)
        assertEquals(12f, map["com.example.heavy"])
        assertEquals(3.5f, map["com.example.light"])
    }

    @Test
    fun `merge drops kiln package sorts by rss and applies limit`() {
        val mem = listOf(
            ProcessStat("com.example.a", "a", 10_000, null, null, 1),
            ProcessStat("dev.involvex.kiln", "Kiln", 99_000, null, null, 2),
            ProcessStat("com.example.b", "b", 50_000, null, null, 3),
        )
        val cpu = mapOf("com.example.b" to 8f)
        val merged = ProcessStatsParser.merge(
            mem = mem,
            cpuByPackage = cpu,
            kilnPackageName = "dev.involvex.kiln",
            limit = 1,
        )
        assertEquals(1, merged.size)
        assertEquals("com.example.b", merged[0].packageName)
        assertEquals(8f, merged[0].cpuPercent)
    }

    @Test
    fun `looksLikePackage rejects shell injection`() {
        assertTrue(ProcessStatsParser.looksLikePackage("com.example.app"))
        assertFalse(ProcessStatsParser.looksLikePackage("bad;rm"))
        assertFalse(ProcessStatsParser.looksLikePackage("nodot"))
    }
}

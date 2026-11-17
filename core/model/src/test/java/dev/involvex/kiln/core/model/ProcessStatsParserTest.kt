package dev.involvex.kiln.core.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ProcessStatsParserTest {
    @Test
    fun `parses classic total pss kB lines`() {
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
    fun `parses HyperOS RSS slash K colon format`() {
        val stdout = """
            Applications Memory Usage (in Kilobytes):

            Total RSS by process:
                880,498K: com.google.android.aicore (pid 12080)
                453,746K: com.mi.android.globallauncher (pid 3003 / activities)
                108,504K: com.android.vending:background (pid 19808)
                 56,984K: com.google.android.webview:sandboxed_process0:org.chromium.content.app.SandboxedProcessService0:0 (pid 24907)
                206,768K: surfaceflinger (pid 1571)

            Total RSS by OOM adjustment:
                skip
        """.trimIndent()
        val rows = ProcessStatsParser.parseMeminfo(stdout)
        assertEquals(4, rows.size)
        assertEquals("com.google.android.aicore", rows[0].packageName)
        assertEquals(880_498L, rows[0].pssKb)
        assertEquals("com.android.vending", rows[2].packageName)
        assertEquals("com.google.android.webview", rows[3].packageName)
        assertFalse(rows.any { it.packageName == "surfaceflinger" })
    }

    @Test
    fun `prefers PSS section when both present`() {
        val stdout = """
            Total RSS by process:
                10,000K: com.example.rss (pid 1)

            Total PSS by process:
              1,251,781K: com.example.pss (pid 2 / activities)

            Total PSS by OOM adjustment:
                end
        """.trimIndent()
        val rows = ProcessStatsParser.parseMeminfo(stdout)
        assertEquals(listOf("com.example.pss"), rows.map { it.packageName })
        assertEquals(1_251_781L, rows[0].pssKb)
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
        assertEquals("com.foo", ProcessStatsParser.basePackageName("com.foo:service"))
    }
}

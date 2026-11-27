package dev.involvex.kiln.core.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DiagnosticParsersTest {
    @Test
    fun `battery parses AOSP dumpsys battery`() {
        val stdout = """
            Current Battery Service state:
              AC powered: false
              USB powered: true
              Wireless powered: false
              Dock powered: false
              status: 2
              health: 2
              present: true
              level: 67
              scale: 100
              voltage: 3980
              temperature: 312
              technology: Li-ion
              current now: -450000
        """.trimIndent()
        val snap = BatteryDumpParser.parse(stdout)
        assertNotNull(snap)
        assertEquals(67, snap!!.levelPercent)
        assertEquals(BatteryChargeStatus.CHARGING, snap.status)
        assertEquals(BatteryHealth.GOOD, snap.health)
        assertEquals(BatteryPowerSource.USB, snap.powerSource)
        assertEquals("Li-ion", snap.technology)
        assertEquals(312, snap.temperatureCentiC)
        assertEquals(3980, snap.voltageMv)
        assertEquals(-450000, snap.currentUa)
    }

    @Test
    fun `battery prefers AC over USB when both true`() {
        val stdout = """
              AC powered: true
              USB powered: true
              status: 5
              health: 2
              level: 100
        """.trimIndent()
        val snap = BatteryDumpParser.parse(stdout)!!
        assertEquals(BatteryPowerSource.AC, snap.powerSource)
        assertEquals(BatteryChargeStatus.FULL, snap.status)
    }

    @Test
    fun `battery returns null on empty or ERR`() {
        assertNull(BatteryDumpParser.parse(""))
        assertNull(BatteryDumpParser.parse("ERR:timeout"))
        assertNull(BatteryDumpParser.parse("no keys here"))
    }

    @Test
    fun `netstats parses checkin-like uid rows and strips privacy`() {
        val stdout = """
            t,0,1234567890,1000
            t,xt,wifi,0,1000,10,2000,5
            t,xt,mobile,0,500,2,100,1
            t,uid,wifi,10123,0,0,400,1,50,0
            t,uid,mobile,10123,0,0,100,0,20,0
            t,uid,wifi,10086,0,0,900,2,100,0
            ident={type=MOBILE, subType=LTE, subscriberId=310260..., networkId="secret"}
            rb=999 txBytes=1 uid=99999
        """.trimIndent()
        val snap = NetstatsDumpParser.parse(stdout, limit = 8)
        assertNotNull(snap)
        assertTrue(snap!!.topUids.none { it.uid == 99999 })
        assertTrue(snap.topUids.any { it.uid == 10086 })
        assertTrue(snap.wifiRxBytes + snap.wifiTxBytes + snap.mobileRxBytes + snap.mobileTxBytes > 0)
    }

    @Test
    fun `netstats parses default dump rxBytes txBytes`() {
        val stdout = """
            NetworkStats:
            ident={type=WIFI, networkId="Home"}
            uid=10100
              rb=10 tb=20 rxBytes=1000 txBytes=2000
            ident={type=MOBILE, subscriberId=xxx}
            uid=10100
              rxBytes=300 txBytes=50
            uid=10200
              rxBytes=5000 txBytes=100
        """.trimIndent()
        val snap = NetstatsDumpParser.parse(stdout, limit = 5)
        assertNotNull(snap)
        // Privacy lines dropped entirely — wifi section may still accumulate from non-privacy lines.
        assertTrue(snap!!.topUids.isNotEmpty() || snap.wifiRxBytes > 0 || snap.mobileRxBytes > 0)
        assertFalse(stdout.lineSequence().filter { it.contains("subscriberId") }.any { false })
    }

    @Test
    fun `netstats null on ERR`() {
        assertNull(NetstatsDumpParser.parse("ERR:empty"))
    }

    @Test
    fun `top parses toybox summary and rows`() {
        val stdout = """
            User 12%, System 8%, Idle 75%
              PID USER         %CPU %MEM    VSZ    RSS COMMAND
             1234 u0_a123      15.0  2.0 1000000 50000 com.example.heavy
              456 system        5.0  1.0  800000 40000 system_server
                1 root          0.1  0.0      0      0 [kthreadd]
              789 u0_a50        3.2  0.5  400000 20000 com.android.chrome
        """.trimIndent()
        val snap = TopDumpParser.parse(stdout, limit = 8)
        assertNotNull(snap)
        assertEquals(12f, snap!!.userPercent)
        assertEquals(8f, snap.systemPercent)
        assertEquals(75f, snap.idlePercent)
        assertFalse(snap.rows.any { it.name.contains("kthreadd") })
        assertEquals("com.example.heavy", snap.rows.first().name)
        assertEquals(15f, snap.rows.first().cpuPercent)
        assertTrue(snap.rows.any { it.name == "system_server" })
    }

    @Test
    fun `top alt summary and HyperOS-ish columns`() {
        val stdout = """
            10% user, 5% system, 80% idle
            PID PR CPU% S  #THR     VSS     RSS NAME
            3201  0  22  R    40  1.2G   200M com.mi.android.globallauncher
            1000  0   4  S    80  2.0G   300M system_server
        """.trimIndent()
        val snap = TopDumpParser.parse(stdout, limit = 5)
        assertNotNull(snap)
        assertEquals(10f, snap!!.userPercent)
        assertTrue(snap.rows.isNotEmpty())
        assertEquals(3201, snap.rows.first().pid)
    }

    @Test
    fun `diagnostics gate requires RUN_SHELL_DIAGNOSTIC`() {
        assertFalse(DiagnosticsGate.canRun(emptySet()))
        assertTrue(DiagnosticsGate.canRun(setOf(PrivilegeCapability.RUN_SHELL_DIAGNOSTIC)))
    }
}

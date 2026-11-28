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
    fun `netstats parses HyperOS checkin wifi buckets`() {
        val stdout = """
            v1,1795671310,1795757710
            xt
            c,wifi,-1,all,0
            d,3600
            b,1795672800,563968382,447592,12850930,91541,0
            b,1795676400,67462604,88891,8849049,56413,0
            c,mobile,-1,all,0
            d,3600
            b,1795672800,1000,1,2000,2,0
        """.trimIndent()
        val snap = NetstatsDumpParser.parse(stdout, limit = 8)
        assertNotNull(snap)
        assertEquals(563968382L + 67462604L, snap!!.wifiRxBytes)
        assertEquals(12850930L + 8849049L, snap.wifiTxBytes)
        assertEquals(1000L, snap.mobileRxBytes)
        assertEquals(2000L, snap.mobileTxBytes)
    }

    @Test
    fun `netstats parses legacy checkin-like uid rows`() {
        val stdout = """
            t,0,1234567890,1000
            t,xt,wifi,0,1000,10,2000,5
            t,xt,mobile,0,500,2,100,1
            t,uid,wifi,10123,0,0,400,1,50,0
            t,uid,wifi,10086,0,0,900,2,100,0
        """.trimIndent()
        val snap = NetstatsDumpParser.parse(stdout, limit = 8)
        assertNotNull(snap)
        assertTrue(snap!!.topUids.any { it.uid == 10086 })
        assertTrue(snap.wifiRxBytes + snap.mobileRxBytes > 0)
    }

    @Test
    fun `netstats parses mAppUidStatsMap and redacts privacy`() {
        val stdout = """
            Active interfaces:
              iface=wlan0 ident=[{type=1, subscriberId=262231..., wifiNetworkKey="Home"}]
            mAppUidStatsMap:
              uid rxBytes rxPackets txBytes txPackets
              10799 738427616 553898 10249044 115535
              1000 161640484 1069953 21788888 124238
              6102 191994 260 55456 291
        """.trimIndent()
        val snap = NetstatsDumpParser.parse(stdout, limit = 5)
        assertNotNull(snap)
        assertEquals(10799, snap!!.topUids.first().uid)
        assertEquals(738427616L, snap.topUids.first().rxBytes)
        assertFalse(snap.topUids.any { it.label.contains("262231") })
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
              11  0   3  S     1     0     0 [kworker/7:11H-+
        """.trimIndent()
        val snap = TopDumpParser.parse(stdout, limit = 5)
        assertNotNull(snap)
        assertEquals(10f, snap!!.userPercent)
        assertTrue(snap.rows.isNotEmpty())
        assertEquals(3201, snap.rows.first().pid)
        assertFalse(snap.rows.any { it.name.startsWith('[') })
    }

    @Test
    fun `diagnostics gate requires RUN_SHELL_DIAGNOSTIC`() {
        assertFalse(DiagnosticsGate.canRun(emptySet()))
        assertTrue(DiagnosticsGate.canRun(setOf(PrivilegeCapability.RUN_SHELL_DIAGNOSTIC)))
    }

    @Test
    fun `cpuinfo parses load and package rows`() {
        val stdout = """
            Load: 1.25 / 2.50 / 3.75
            CPU usage from 10000ms to 0ms ago:
              12% 1000/com.example.heavy: 10% user + 2% kernel
               3.5% 99/com.example.light: 3% user + 0.5% kernel
        """.trimIndent()
        val snap = CpuinfoDumpParser.parse(stdout, limit = 8)
        assertNotNull(snap)
        assertEquals(1.25f, snap!!.load1)
        assertEquals(2.5f, snap.load5)
        assertEquals(3.75f, snap.load15)
        assertEquals("com.example.heavy", snap.rows.first().packageName)
        assertEquals(12f, snap.rows.first().cpuPercent)
        assertEquals(3.5f, snap.rows[1].cpuPercent)
    }

    @Test
    fun `cpuinfo null on ERR`() {
        assertNull(CpuinfoDumpParser.parse("ERR:empty"))
    }

    @Test
    fun `thermal parses HAL temps and skips sentinel`() {
        val stdout = """
            IsStatusOverride: false
            Thermal Status: 2
            Current temperatures from HAL:
              Temperature{mValue=42.5, mType=3, mName=CPU, mStatus=0}
              Temperature{mValue=-99.9, mType=1, mName=broken, mStatus=0}
              Temperature{mValue=36.0, mType=2, mName=GPU, mStatus=0}
            Cached temperatures:
              Temperature{mValue=99.0, mType=3, mName=CPU, mStatus=0}
        """.trimIndent()
        val snap = ThermalDumpParser.parse(stdout)
        assertNotNull(snap)
        assertEquals(ThermalStatusLevel.MODERATE, snap!!.status)
        assertEquals(2, snap.zones.size)
        assertEquals("CPU", snap.zones.first().name)
        assertEquals(42.5f, snap.zones.first().celsius)
        assertFalse(snap.zones.any { it.name == "broken" })
    }

    @Test
    fun `thermal falls back to cached when HAL missing`() {
        val stdout = """
            Thermal Status: 0
            Cached temperatures:
              Temperature{mValue=31.0, mType=0, mName=skin, mStatus=0}
        """.trimIndent()
        val snap = ThermalDumpParser.parse(stdout)
        assertNotNull(snap)
        assertEquals(ThermalStatusLevel.NONE, snap!!.status)
        assertEquals("skin", snap.zones.single().name)
    }
}

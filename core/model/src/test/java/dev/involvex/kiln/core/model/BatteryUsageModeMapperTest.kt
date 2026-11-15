package dev.involvex.kiln.core.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BatteryUsageModeMapperTest {
    @Test
    fun fromShell_mapsTristate() {
        assertEquals(
            BatteryUsageMode.UNRESTRICTED,
            BatteryUsageModeMapper.fromShell(whitelisted = true, runAnyMode = "allow"),
        )
        assertEquals(
            BatteryUsageMode.OPTIMIZED,
            BatteryUsageModeMapper.fromShell(whitelisted = false, runAnyMode = "allow"),
        )
        assertEquals(
            BatteryUsageMode.RESTRICTED,
            BatteryUsageModeMapper.fromShell(whitelisted = false, runAnyMode = "ignore"),
        )
    }

    @Test
    fun toShell_roundTrip() {
        assertEquals("+" to "allow", BatteryUsageModeMapper.toShell(BatteryUsageMode.UNRESTRICTED))
        assertEquals("-" to "allow", BatteryUsageModeMapper.toShell(BatteryUsageMode.OPTIMIZED))
        assertEquals("-" to "ignore", BatteryUsageModeMapper.toShell(BatteryUsageMode.RESTRICTED))
    }

    @Test
    fun appOpOutputParser_readsMode() {
        assertEquals("allow", AppOpOutputParser.parseMode("Uid mode: allow"))
        assertEquals("ignore", AppOpOutputParser.parseMode("10008: mode=ignore; override"))
        assertTrue(AppOpOutputParser.isAllowed("mode: allow"))
        assertFalse(AppOpOutputParser.isAllowed("mode: ignore"))
    }

    @Test
    fun appOpOutputParser_autostartMatchesHyperOsSettings() {
        assertTrue(AppOpOutputParser.isAutostartAllowed("10008: mode=allow"))
        assertTrue(AppOpOutputParser.isAutostartAllowed("10008: mode=default; override"))
        assertFalse(AppOpOutputParser.isAutostartAllowed("10008: mode=ignore"))
        assertFalse(AppOpOutputParser.isAutostartAllowed(""))
        // Strict allow still requires explicit allow (battery / other ops).
        assertFalse(AppOpOutputParser.isAllowed("mode: default"))
    }

    @Test
    fun deviceIdleWhitelistParser() {
        assertTrue(DeviceIdleWhitelistParser.isWhitelisted("com.example.app", "true"))
        assertFalse(DeviceIdleWhitelistParser.isWhitelisted("com.example.app", "false"))
        assertTrue(DeviceIdleWhitelistParser.isWhitelisted("com.example.app", "com.example.app"))
        assertFalse(DeviceIdleWhitelistParser.isWhitelisted("com.example.app", "com.other.app"))
    }
}

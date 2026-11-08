package dev.involvex.kiln.feature.sources.updatecheck

import androidx.work.NetworkType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UpdateCheckConstraintsTest {
    @Test
    fun periodic_requires_battery_not_low() {
        val prefs = dev.involvex.kiln.core.model.BackgroundUpdatePrefs(
            enabled = true,
            unmeteredOnly = false,
        )
        val c = UpdateCheckConstraints.from(prefs)
        assertTrue(c.requiresBatteryNotLow())
        assertEquals(NetworkType.CONNECTED, c.requiredNetworkType)
    }

    @Test
    fun expedited_immediate_has_no_battery_constraint() {
        val c = UpdateCheckConstraints.forExpeditedImmediate(unmeteredOnly = false)
        assertFalse(c.requiresBatteryNotLow())
        assertEquals(NetworkType.CONNECTED, c.requiredNetworkType)
    }
}

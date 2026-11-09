package dev.involvex.kiln.feature.maintenance

import androidx.work.NetworkType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MaintenanceConstraintsTest {
    @Test
    fun periodic_requires_unmetered_and_charging() {
        val c = MaintenanceConstraints.forPeriodic()
        assertEquals(NetworkType.UNMETERED, c.requiredNetworkType)
        assertTrue(c.requiresCharging())
    }

    @Test
    fun immediate_does_not_require_charging() {
        val c = MaintenanceConstraints.forImmediate()
        assertEquals(NetworkType.NOT_REQUIRED, c.requiredNetworkType)
        assertFalse(c.requiresCharging())
    }
}

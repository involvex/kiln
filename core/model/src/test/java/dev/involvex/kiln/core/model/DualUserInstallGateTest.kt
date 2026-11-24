package dev.involvex.kiln.core.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DualUserInstallGateTest {
    private val users = listOf(
        AndroidUserInfo(id = 0, name = "Owner", running = true),
        AndroidUserInfo(id = 10, name = "Work", running = false),
    )

    @Test
    fun showPicker_requires_silent_and_at_least_two_users() {
        assertFalse(DualUserInstallGate.showPicker(users, hasSilentCapability = false))
        assertFalse(DualUserInstallGate.showPicker(users.take(1), hasSilentCapability = true))
        assertFalse(DualUserInstallGate.showPicker(emptyList(), hasSilentCapability = true))
        assertTrue(DualUserInstallGate.showPicker(users, hasSilentCapability = true))
    }

    @Test
    fun resolveTargetUserId_falls_back_to_current() {
        assertEquals(0, DualUserInstallGate.resolveTargetUserId(null, users, currentUserId = 0))
        assertEquals(10, DualUserInstallGate.resolveTargetUserId(10, users, currentUserId = 0))
        assertEquals(0, DualUserInstallGate.resolveTargetUserId(99, users, currentUserId = 0))
        assertEquals(0, DualUserInstallGate.resolveTargetUserId(10, emptyList(), currentUserId = 0))
    }
}

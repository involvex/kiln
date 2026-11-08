package dev.involvex.kiln.core.privilege

import dev.involvex.kiln.core.model.CapabilityAvailability
import dev.involvex.kiln.core.model.PrivilegeCapability
import dev.involvex.kiln.core.model.PrivilegeTier
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CapabilityMatrixTest {
    @Test
    fun shell_uid_enables_silent_install_not_full_backup() {
        val matrix = CapabilityMatrix.fromUid(CapabilityMatrix.SHELL_UID, PrivilegeTier.SHIZUKU)
        assertEquals(CapabilityAvailability.AVAILABLE, matrix[PrivilegeCapability.SILENT_INSTALL])
        assertEquals(CapabilityAvailability.AVAILABLE, matrix[PrivilegeCapability.DEBLOAT])
        assertEquals(CapabilityAvailability.RESTRICTED, matrix[PrivilegeCapability.FULL_BACKUP])
        assertTrue(PrivilegeCapability.SILENT_INSTALL in CapabilityMatrix.capabilities(2000, PrivilegeTier.SHIZUKU))
        assertTrue(PrivilegeCapability.FULL_BACKUP !in CapabilityMatrix.capabilities(2000, PrivilegeTier.SHIZUKU))
    }

    @Test
    fun root_uid_enables_full_backup() {
        val matrix = CapabilityMatrix.fromUid(CapabilityMatrix.ROOT_UID, PrivilegeTier.SHIZUKU)
        assertEquals(CapabilityAvailability.AVAILABLE, matrix[PrivilegeCapability.FULL_BACKUP])
    }

    @Test
    fun none_tier_all_unavailable() {
        val matrix = CapabilityMatrix.fromUid(2000, PrivilegeTier.NONE)
        assertTrue(matrix.values.all { it == CapabilityAvailability.UNAVAILABLE })
    }
}

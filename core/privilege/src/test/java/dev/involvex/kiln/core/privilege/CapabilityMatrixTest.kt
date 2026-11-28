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
        assertEquals(CapabilityAvailability.AVAILABLE, matrix[PrivilegeCapability.MANAGE_APP_OPS])
        assertEquals(CapabilityAvailability.RESTRICTED, matrix[PrivilegeCapability.FULL_BACKUP])
        assertTrue(PrivilegeCapability.SILENT_INSTALL in CapabilityMatrix.capabilities(2000, PrivilegeTier.SHIZUKU))
        assertTrue(PrivilegeCapability.MANAGE_APP_OPS in CapabilityMatrix.capabilities(2000, PrivilegeTier.SHIZUKU))
        assertTrue(PrivilegeCapability.FORCE_STOP in CapabilityMatrix.capabilities(2000, PrivilegeTier.SHIZUKU))
        assertTrue(PrivilegeCapability.RUN_SHELL_DIAGNOSTIC in CapabilityMatrix.capabilities(2000, PrivilegeTier.SHIZUKU))
        assertTrue(PrivilegeCapability.FULL_BACKUP !in CapabilityMatrix.capabilities(2000, PrivilegeTier.SHIZUKU))
    }

    @Test
    fun root_uid_enables_full_backup() {
        val matrix = CapabilityMatrix.fromUid(CapabilityMatrix.ROOT_UID, PrivilegeTier.SHIZUKU)
        assertEquals(CapabilityAvailability.AVAILABLE, matrix[PrivilegeCapability.FULL_BACKUP])
        val rootTier = CapabilityMatrix.fromUid(CapabilityMatrix.ROOT_UID, PrivilegeTier.ROOT)
        assertEquals(CapabilityAvailability.AVAILABLE, rootTier[PrivilegeCapability.FULL_BACKUP])
        assertTrue(PrivilegeCapability.FULL_BACKUP in CapabilityMatrix.capabilities(0, PrivilegeTier.ROOT))
    }

    @Test
    fun none_tier_all_unavailable() {
        val matrix = CapabilityMatrix.fromUid(2000, PrivilegeTier.NONE)
        assertTrue(matrix.values.all { it == CapabilityAvailability.UNAVAILABLE })
    }
}

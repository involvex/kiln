package dev.involvex.kiln.feature.permissions

import dev.involvex.kiln.core.model.PermissionAuditGate
import dev.involvex.kiln.core.model.PrivilegeCapability
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Destructive-path gate: mutate actions must not run without MANAGE_APP_OPS. */
class PermissionAuditGateTest {
    @Test
    fun mutate_requires_manage_app_ops_capability() {
        assertFalse(PermissionAuditGate.canMutate(emptySet()))
        assertFalse(PermissionAuditGate.canMutate(setOf(PrivilegeCapability.CLEAR_CACHE)))
        assertTrue(PermissionAuditGate.canMutate(setOf(PrivilegeCapability.MANAGE_APP_OPS)))
    }
}

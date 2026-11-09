package dev.involvex.kiln.core.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PermissionAuditFilterTest {
    private val samples = listOf(
        PermissionAuditApp(
            packageName = "com.example.cam",
            label = "Camera App",
            isSystemApp = false,
            dangerousGranted = listOf(
                AppPermissionEntry("android.permission.CAMERA", granted = true, isDangerous = true),
            ),
        ),
        PermissionAuditApp(
            packageName = "com.example.clean",
            label = "Clean App",
            isSystemApp = false,
            dangerousGranted = emptyList(),
        ),
        PermissionAuditApp(
            packageName = "com.vendor.loc",
            label = "Vendor Location",
            isSystemApp = true,
            dangerousGranted = listOf(
                AppPermissionEntry("android.permission.ACCESS_FINE_LOCATION", granted = true, isDangerous = true),
                AppPermissionEntry("android.permission.ACCESS_COARSE_LOCATION", granted = true, isDangerous = true),
            ),
        ),
    )

    @Test
    fun withDangerousGranted_drops_empty() {
        val result = PermissionAuditFilter.withDangerousGranted(samples)
        assertEquals(2, result.size)
        assertEquals(3, PermissionAuditFilter.countDangerousGranted(result))
    }

    @Test
    fun filter_matches_label_package_or_permission() {
        assertEquals(1, PermissionAuditFilter.filter(samples, "camera").size)
        assertEquals(1, PermissionAuditFilter.filter(samples, "vendor.loc").size)
        assertEquals(1, PermissionAuditFilter.filter(samples, "FINE_LOCATION").size)
        assertEquals(3, PermissionAuditFilter.filter(samples, "").size)
    }

    @Test
    fun gate_requires_manage_app_ops() {
        assertFalse(PermissionAuditGate.canMutate(emptySet()))
        assertTrue(PermissionAuditGate.canMutate(setOf(PrivilegeCapability.MANAGE_APP_OPS)))
        assertFalse(PermissionAuditGate.canMutate(setOf(PrivilegeCapability.DEBLOAT)))
    }
}

package dev.involvex.kiln.core.model

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ProtectedPackagesTest {
    @Test
    fun hardBlocked_covers_shizuku_and_system_ui() {
        assertTrue(ProtectedPackages.isHardBlocked("com.android.systemui"))
        assertTrue(ProtectedPackages.isHardBlocked("com.android.settings"))
        assertTrue(ProtectedPackages.isHardBlocked("moe.shizuku.privileged.api"))
        assertTrue(ProtectedPackages.isHardBlocked("af.shizuku.plus.api"))
        assertFalse(ProtectedPackages.isHardBlocked("com.example.harmless"))
    }
}

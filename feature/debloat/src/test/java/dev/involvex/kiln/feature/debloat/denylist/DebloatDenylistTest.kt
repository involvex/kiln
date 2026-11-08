package dev.involvex.kiln.feature.debloat.denylist

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DebloatDenylistTest {
    @Test
    fun hard_blocks_systemui_settings_shizuku() {
        assertTrue(DebloatDenylist.isHardBlocked("com.android.systemui"))
        assertTrue(DebloatDenylist.isHardBlocked("com.android.settings"))
        assertTrue(DebloatDenylist.isHardBlocked("moe.shizuku.privileged.api"))
        assertTrue(DebloatDenylist.isHardBlocked("af.shizuku.plus.api"))
        assertFalse(DebloatDenylist.isHardBlocked("com.example.harmless"))
    }
}

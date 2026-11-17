package dev.involvex.kiln.core.privilege.shell

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MeminfoSnapshotTest {
    @Test
    fun `detects rss and pss process section starts`() {
        assertTrue(MeminfoSnapshot.isProcessSectionStart("Total RSS by process:"))
        assertTrue(MeminfoSnapshot.isProcessSectionStart("  Total PSS by process:"))
        assertFalse(MeminfoSnapshot.isProcessSectionStart("Total RSS by OOM adjustment:"))
    }

    @Test
    fun `ends section on next total header`() {
        assertTrue(MeminfoSnapshot.isProcessSectionEnd("Total RSS by OOM adjustment:"))
        assertTrue(MeminfoSnapshot.isProcessSectionEnd("Total PSS by category:"))
        assertFalse(MeminfoSnapshot.isProcessSectionEnd("Total PSS by process:"))
        assertFalse(MeminfoSnapshot.isProcessSectionEnd("    12,000K: com.example.app (pid 1)"))
    }
}

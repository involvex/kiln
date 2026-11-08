package dev.involvex.kiln.feature.installer

import dev.involvex.kiln.core.model.PreferredInstaller
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class InstallCommitPolicyTest {
    @Test
    fun system_never_silent() {
        assertFalse(
            InstallCommitPolicy.useSilentInstall(PreferredInstaller.SYSTEM, hasSilentCapability = true),
        )
    }

    @Test
    fun shizuku_requires_capability() {
        assertFalse(
            InstallCommitPolicy.useSilentInstall(PreferredInstaller.SHIZUKU, hasSilentCapability = false),
        )
        assertTrue(
            InstallCommitPolicy.useSilentInstall(PreferredInstaller.SHIZUKU, hasSilentCapability = true),
        )
    }
}

package dev.involvex.kiln.feature.installer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class InstallPackageNameTest {
    @Test
    fun prefersApkPackageName() {
        assertEquals(
            "com.termux.styling",
            InstallPackageName.resolve("github.termux.termuxstyling", "com.termux.styling"),
        )
    }

    @Test
    fun fallsBackToJobWhenApkUnknown() {
        assertEquals(
            "github.termux.termuxstyling",
            InstallPackageName.resolve("github.termux.termuxstyling", null),
        )
        assertEquals(
            "github.termux.termuxstyling",
            InstallPackageName.resolve("github.termux.termuxstyling", "  "),
        )
    }

    @Test
    fun detectsProvisionalGithubIds() {
        assertTrue(InstallPackageName.isProvisional("github.termux.termuxstyling"))
        assertFalse(InstallPackageName.isProvisional("com.termux.styling"))
    }
}

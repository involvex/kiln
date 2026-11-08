package dev.involvex.kiln.core.privilege.shell

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ShellAllowlistTest {
    @Test
    fun id_rejects_extra_args() {
        assertNull(ShellAllowlist.buildArgv(ShellAllowlist.ID, listOf("x")))
        assertEquals(listOf("id"), ShellAllowlist.buildArgv(ShellAllowlist.ID, emptyList()))
    }

    @Test
    fun pm_install_accepts_safe_path() {
        val path = "/data/user/0/dev.involvex.kiln/cache/install/job.apk"
        assertEquals(
            listOf("pm", "install", "-r", "--user", "0", path),
            ShellAllowlist.buildArgv(ShellAllowlist.PM_INSTALL, listOf(path)),
        )
    }

    @Test
    fun pm_install_rejects_injection() {
        assertNull(
            ShellAllowlist.buildArgv(
                ShellAllowlist.PM_INSTALL,
                listOf("/tmp/a.apk; rm -rf /"),
            ),
        )
        assertNull(
            ShellAllowlist.buildArgv(
                ShellAllowlist.PM_INSTALL,
                listOf("/tmp/../etc/passwd.apk"),
            ),
        )
        assertNull(
            ShellAllowlist.buildArgv(
                ShellAllowlist.PM_INSTALL,
                listOf("relative.apk"),
            ),
        )
    }

    @Test
    fun pm_uninstall_keep_data_flag() {
        val pkg = "com.example.app"
        assertEquals(
            listOf("pm", "uninstall", "-k", "--user", "0", pkg),
            ShellAllowlist.buildArgv(ShellAllowlist.PM_UNINSTALL, listOf(pkg, "keep")),
        )
        assertEquals(
            listOf("pm", "uninstall", "--user", "0", pkg),
            ShellAllowlist.buildArgv(ShellAllowlist.PM_UNINSTALL, listOf(pkg)),
        )
    }

    @Test
    fun package_name_validation() {
        assertTrue(ShellAllowlist.isSafePackageName("com.example.app"))
        assertFalse(ShellAllowlist.isSafePackageName("com.example.app;rm"))
        assertFalse(ShellAllowlist.isSafePackageName("../evil"))
        assertFalse(ShellAllowlist.isSafePackageName("a"))
    }

    @Test
    fun unknown_command_rejected() {
        assertNull(ShellAllowlist.buildArgv("rm", listOf("-rf", "/")))
    }
}

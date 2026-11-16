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
    fun pm_install_downgrade_includes_d_flag() {
        val path = "/data/user/0/dev.involvex.kiln/files/apk_rollback/com.example/10.apk"
        assertEquals(
            listOf("pm", "install", "-r", "-d", "--user", "0", path),
            ShellAllowlist.buildArgv(ShellAllowlist.PM_INSTALL_DOWNGRADE, listOf(path)),
        )
    }

    @Test
    fun pm_install_multiple_accepts_safe_paths() {
        val base = "/data/user/0/dev.involvex.kiln/cache/apk_downloads/job/base.apk"
        val abi = "/data/user/0/dev.involvex.kiln/cache/apk_downloads/job/config.arm64_v8a.apk"
        assertEquals(
            listOf("pm", "install-multiple", "-r", "--user", "0", base, abi),
            ShellAllowlist.buildArgv(ShellAllowlist.PM_INSTALL_MULTIPLE, listOf(base, abi)),
        )
        assertEquals(
            listOf("pm", "install-multiple", "-r", "-d", "--user", "0", base, abi),
            ShellAllowlist.buildArgv(ShellAllowlist.PM_INSTALL_MULTIPLE_DOWNGRADE, listOf(base, abi)),
        )
    }

    @Test
    fun pm_install_multiple_rejects_bad_path_or_empty() {
        assertNull(ShellAllowlist.buildArgv(ShellAllowlist.PM_INSTALL_MULTIPLE, emptyList()))
        assertNull(
            ShellAllowlist.buildArgv(
                ShellAllowlist.PM_INSTALL_MULTIPLE,
                listOf("/data/ok.apk", "/tmp/a.apk;rm"),
            ),
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

    @Test
    fun pm_clear_cache_and_trim() {
        val pkg = "com.example.app"
        assertEquals(
            listOf("pm", "clear", "--cache-only", pkg),
            ShellAllowlist.buildArgv(ShellAllowlist.PM_CLEAR_CACHE, listOf(pkg)),
        )
        assertNull(ShellAllowlist.buildArgv(ShellAllowlist.PM_CLEAR_CACHE, listOf("bad;pkg")))
        assertEquals(
            listOf("pm", "trim-caches", "999999999999"),
            ShellAllowlist.buildArgv(ShellAllowlist.PM_TRIM_CACHES, listOf("999999999999")),
        )
        assertNull(ShellAllowlist.buildArgv(ShellAllowlist.PM_TRIM_CACHES, listOf("-1")))
        assertNull(ShellAllowlist.buildArgv(ShellAllowlist.PM_TRIM_CACHES, listOf("999G")))
    }

    @Test
    fun pm_debloat_commands() {
        val pkg = "com.example.bloat"
        assertEquals(
            listOf("pm", "disable-user", "--user", "0", pkg),
            ShellAllowlist.buildArgv(ShellAllowlist.PM_DISABLE_USER, listOf(pkg)),
        )
        assertEquals(
            listOf("pm", "enable", "--user", "0", pkg),
            ShellAllowlist.buildArgv(ShellAllowlist.PM_ENABLE, listOf(pkg)),
        )
        assertEquals(
            listOf("pm", "install-existing", "--user", "0", pkg),
            ShellAllowlist.buildArgv(ShellAllowlist.PM_INSTALL_EXISTING, listOf(pkg)),
        )
        assertNull(ShellAllowlist.buildArgv(ShellAllowlist.PM_DISABLE_USER, listOf("a;b")))
    }

    @Test
    fun suspendAndUnsuspend_requireSafePackage() {
        val pkg = "com.example.app"
        assertEquals(
            listOf("pm", "suspend", "--user", "0", pkg),
            ShellAllowlist.buildArgv(ShellAllowlist.PM_SUSPEND, listOf(pkg)),
        )
        assertEquals(
            listOf("pm", "unsuspend", "--user", "0", pkg),
            ShellAllowlist.buildArgv(ShellAllowlist.PM_UNSUSPEND, listOf(pkg)),
        )
        assertNull(ShellAllowlist.buildArgv(ShellAllowlist.PM_SUSPEND, listOf("bad;pkg")))
        assertNull(ShellAllowlist.buildArgv(ShellAllowlist.PM_UNSUSPEND, listOf("a")))
    }

    @Test
    fun pm_revoke_accepts_safe_permission() {
        val pkg = "com.example.app"
        val perm = "android.permission.CAMERA"
        assertEquals(
            listOf("pm", "revoke", pkg, perm),
            ShellAllowlist.buildArgv(ShellAllowlist.PM_REVOKE, listOf(pkg, perm)),
        )
        assertNull(ShellAllowlist.buildArgv(ShellAllowlist.PM_REVOKE, listOf(pkg)))
        assertNull(ShellAllowlist.buildArgv(ShellAllowlist.PM_REVOKE, listOf(pkg, "CAMERA;rm")))
        assertNull(ShellAllowlist.buildArgv(ShellAllowlist.PM_REVOKE, listOf("bad;pkg", perm)))
        assertFalse(ShellAllowlist.isSafePermissionName("short"))
        assertTrue(ShellAllowlist.isSafePermissionName(perm))
    }

    @Test
    fun appops_set_allowlist_only() {
        val pkg = "com.example.app"
        assertEquals(
            listOf("appops", "set", pkg, "CAMERA", "ignore"),
            ShellAllowlist.buildArgv(ShellAllowlist.APPOPS_SET, listOf(pkg, "CAMERA", "ignore")),
        )
        assertNull(ShellAllowlist.buildArgv(ShellAllowlist.APPOPS_SET, listOf(pkg, "CAMERA", "deny")))
        assertNull(ShellAllowlist.buildArgv(ShellAllowlist.APPOPS_SET, listOf(pkg, "WAKE_LOCK", "ignore")))
        assertNull(ShellAllowlist.buildArgv(ShellAllowlist.APPOPS_SET, listOf(pkg, "CAMERA")))
        assertTrue(ShellAllowlist.isAllowedAppOp("RECORD_AUDIO"))
        assertFalse(ShellAllowlist.isAllowedAppOpMode("deny"))
    }

    @Test
    fun appops_get_and_numeric_background_ops() {
        val pkg = "com.example.app"
        assertEquals(
            listOf("appops", "get", pkg, "10008"),
            ShellAllowlist.buildArgv(ShellAllowlist.APPOPS_GET, listOf(pkg, "10008")),
        )
        assertEquals(
            listOf("appops", "set", pkg, "10021", "allow"),
            ShellAllowlist.buildArgv(ShellAllowlist.APPOPS_SET, listOf(pkg, "10021", "allow")),
        )
        assertEquals(
            listOf("appops", "set", pkg, "RUN_ANY_IN_BACKGROUND", "ignore"),
            ShellAllowlist.buildArgv(ShellAllowlist.APPOPS_SET, listOf(pkg, "RUN_ANY_IN_BACKGROUND", "ignore")),
        )
        assertNull(ShellAllowlist.buildArgv(ShellAllowlist.APPOPS_GET, listOf(pkg, "99999")))
        assertNull(ShellAllowlist.buildArgv(ShellAllowlist.APPOPS_GET, listOf(pkg)))
        assertTrue(ShellAllowlist.isAllowedAppOp("10008"))
    }

    @Test
    fun deviceidle_whitelist_allowlist() {
        val pkg = "com.example.app"
        assertEquals(
            listOf("dumpsys", "deviceidle", "whitelist", "+$pkg"),
            ShellAllowlist.buildArgv(ShellAllowlist.DEVICEIDLE_WHITELIST, listOf("+", pkg)),
        )
        assertEquals(
            listOf("dumpsys", "deviceidle", "whitelist", "-$pkg"),
            ShellAllowlist.buildArgv(ShellAllowlist.DEVICEIDLE_WHITELIST, listOf("-", pkg)),
        )
        assertEquals(
            listOf("dumpsys", "deviceidle", "whitelist", "=$pkg"),
            ShellAllowlist.buildArgv(ShellAllowlist.DEVICEIDLE_WHITELIST, listOf("=", pkg)),
        )
        assertNull(ShellAllowlist.buildArgv(ShellAllowlist.DEVICEIDLE_WHITELIST, listOf("*", pkg)))
        assertNull(ShellAllowlist.buildArgv(ShellAllowlist.DEVICEIDLE_WHITELIST, listOf("+", "bad;pkg")))
    }

    @Test
    fun android_shared_fs_allowlist() {
        val pkg = "com.example.gone"
        assertEquals(
            listOf("ls", "-1", "/storage/emulated/0/Android/data"),
            ShellAllowlist.buildArgv(ShellAllowlist.LS_ANDROID_ROOT, listOf("data")),
        )
        assertEquals(
            listOf("du", "-s", "/storage/emulated/0/Android/obb/$pkg"),
            ShellAllowlist.buildArgv(ShellAllowlist.DU_ANDROID_PKG, listOf("obb", pkg)),
        )
        assertEquals(
            listOf("rm", "-rf", "/storage/emulated/0/Android/data/$pkg"),
            ShellAllowlist.buildArgv(ShellAllowlist.RM_ANDROID_PKG, listOf("data", pkg)),
        )
        assertNull(ShellAllowlist.buildArgv(ShellAllowlist.LS_ANDROID_ROOT, listOf("sdcard")))
        assertNull(ShellAllowlist.buildArgv(ShellAllowlist.RM_ANDROID_PKG, listOf("data", "../evil")))
        assertNull(ShellAllowlist.buildArgv(ShellAllowlist.RM_ANDROID_PKG, listOf("data", "com.example;rm")))
        assertNull(ShellAllowlist.androidPackagePath("data", "a/b"))
    }

    @Test
    fun tar_android_pkg_and_force_stop_allowlist() {
        val pkg = "com.example.app"
        val tar = "/storage/emulated/0/Android/data/dev.involvex.kiln/cache/backup_stage/data.tar"
        assertEquals(
            listOf("tar", "-cf", tar, "-C", "/storage/emulated/0/Android/data", pkg),
            ShellAllowlist.buildArgv(ShellAllowlist.TAR_ANDROID_PKG_OUT, listOf("data", pkg, tar)),
        )
        assertEquals(
            listOf("tar", "-xf", tar, "-C", "/storage/emulated/0/Android/obb"),
            ShellAllowlist.buildArgv(ShellAllowlist.TAR_ANDROID_PKG_IN, listOf("obb", pkg, tar)),
        )
        assertEquals(
            listOf("am", "force-stop", pkg),
            ShellAllowlist.buildArgv(ShellAllowlist.AM_FORCE_STOP, listOf(pkg)),
        )
        assertNull(
            ShellAllowlist.buildArgv(
                ShellAllowlist.TAR_ANDROID_PKG_OUT,
                listOf("data", pkg, "/tmp/evil.tar"),
            ),
        )
        assertNull(ShellAllowlist.buildArgv(ShellAllowlist.AM_FORCE_STOP, listOf("bad;pkg")))
    }
}

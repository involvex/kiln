package dev.involvex.kiln.core.privilege.shell

import dev.involvex.kiln.core.model.AndroidSharedRoot
import dev.involvex.kiln.core.model.BackgroundControlsOps
import dev.involvex.kiln.core.model.PermissionAppOpsAllowlist

/**
 * Fixed allowlist for privileged shell invocations.
 * Arguments are validated; never concatenate user input into a shell string.
 */
object ShellAllowlist {
    const val ID = "id"
    const val PM_INSTALL = "pm_install"
    const val PM_INSTALL_DOWNGRADE = "pm_install_downgrade"
    const val PM_INSTALL_MULTIPLE = "pm_install_multiple"
    const val PM_INSTALL_MULTIPLE_DOWNGRADE = "pm_install_multiple_downgrade"
    const val PM_UNINSTALL = "pm_uninstall"
    const val PM_CLEAR_CACHE = "pm_clear_cache"
    const val PM_TRIM_CACHES = "pm_trim_caches"
    const val PM_DISABLE_USER = "pm_disable_user"
    const val PM_ENABLE = "pm_enable"
    const val PM_ENABLE_COMPONENT = "pm_enable_component"
    const val PM_DISABLE_COMPONENT = "pm_disable_component"
    const val PM_LIST_USERS = "pm_list_users"
    const val PM_INSTALL_EXISTING = "pm_install_existing"
    const val PM_SUSPEND = "pm_suspend"
    const val PM_UNSUSPEND = "pm_unsuspend"
    const val PM_REVOKE = "pm_revoke"
    const val APPOPS_SET = "appops_set"
    const val APPOPS_GET = "appops_get"
    const val DEVICEIDLE_WHITELIST = "deviceidle_whitelist"
    const val LS_ANDROID_ROOT = "ls_android_root"
    const val DU_ANDROID_PKG = "du_android_pkg"
    const val RM_ANDROID_PKG = "rm_android_pkg"
    const val TAR_ANDROID_PKG_OUT = "tar_android_pkg_out"
    const val TAR_ANDROID_PKG_IN = "tar_android_pkg_in"
    const val AM_FORCE_STOP = "am_force_stop"
    const val DUMPSYS_MEMINFO = "dumpsys_meminfo"
    const val DUMPSYS_CPUINFO = "dumpsys_cpuinfo"
    const val DUMPSYS_BATTERY = "dumpsys_battery"
    const val DUMPSYS_NETSTATS = "dumpsys_netstats"
    const val DUMPSYS_THERMALSERVICE = "dumpsys_thermalservice"
    const val TOP_ONCE = "top_once"

    private val PACKAGE_NAME = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$")
    private val PERMISSION_NAME = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$")

    /** Component class: absolute, relative (`.Foo`), or inner (`Foo$Bar`). */
    private val COMPONENT_CLASS = Regex("^[a-zA-Z_.][a-zA-Z0-9_$.]*$")
    private val APK_PATH = Regex("^/[\\w./\\-+]+\\.apk$", RegexOption.IGNORE_CASE)

    /** Staging tar under Kiln app external cache only. */
    private val STAGING_TAR_PATH = Regex(
        "^/storage/emulated/0/Android/data/dev\\.involvex\\.kiln(\\.debug)?/cache/[\\w./\\-]+\\.tar$",
        RegexOption.IGNORE_CASE,
    )
    private val DEVICEIDLE_ACTIONS: Set<String> = setOf("+", "-", "=")

    /** Desired free-space argument for `pm trim-caches` (digits only, capped length). */
    private val TRIM_BYTES = Regex("^[1-9][0-9]{0,18}$")

    /** Android user id for `--user` (digits only; never free text). */
    private val USER_ID = Regex("^[0-9]{1,5}$")

    val ALLOWED_APP_OPS: Set<String> get() = PermissionAppOpsAllowlist.OPS + BackgroundControlsOps.ALL_OPS

    val ALLOWED_APP_OP_MODES: Set<String> get() = PermissionAppOpsAllowlist.MODES + BackgroundControlsOps.MODES

    /**
     * Builds argv for [ProcessBuilder], or null if [commandId]/[args] are rejected.
     */
    fun buildArgv(commandId: String, args: List<String>): List<String>? = when (commandId) {
        ID -> {
            if (args.isNotEmpty()) null else listOf("id")
        }

        PM_INSTALL -> {
            val path = args.getOrNull(0) ?: return null
            val userId = optionalUserId(args, index = 1) ?: return null
            if (args.size > 2) return null
            if (!isSafeApkPath(path)) null else listOf("pm", "install", "-r", "--user", userId, path)
        }

        PM_INSTALL_DOWNGRADE -> {
            val path = args.getOrNull(0) ?: return null
            val userId = optionalUserId(args, index = 1) ?: return null
            if (args.size > 2) return null
            // -d allows installing an older versionCode over a newer one (rollback).
            if (!isSafeApkPath(path)) null else listOf("pm", "install", "-r", "-d", "--user", userId, path)
        }

        PM_INSTALL_MULTIPLE -> buildInstallMultipleArgv(args, downgrade = false)

        PM_INSTALL_MULTIPLE_DOWNGRADE -> buildInstallMultipleArgv(args, downgrade = true)

        PM_UNINSTALL -> {
            val packageName = args.getOrNull(0) ?: return null
            if (!isSafePackageName(packageName)) return null
            val keepData = args.getOrNull(1) == "keep"
            val userIndex = if (keepData) 2 else 1
            val userId = optionalUserId(args, index = userIndex) ?: return null
            if (args.size > userIndex + 1) return null
            if (keepData) {
                listOf("pm", "uninstall", "-k", "--user", userId, packageName)
            } else {
                listOf("pm", "uninstall", "--user", userId, packageName)
            }
        }

        PM_CLEAR_CACHE -> {
            val packageName = args.singleOrNull() ?: return null
            // API 26+: --cache-only; HyperOS/MIUI may still reject — caller falls back to settings.
            if (!isSafePackageName(packageName)) {
                null
            } else {
                listOf("pm", "clear", "--cache-only", packageName)
            }
        }

        PM_TRIM_CACHES -> {
            val desired = args.singleOrNull() ?: return null
            if (!TRIM_BYTES.matches(desired)) null else listOf("pm", "trim-caches", desired)
        }

        PM_DISABLE_USER -> {
            val packageName = args.getOrNull(0) ?: return null
            val userId = optionalUserId(args, index = 1) ?: return null
            if (args.size > 2) return null
            if (!isSafePackageName(packageName)) {
                null
            } else {
                listOf("pm", "disable-user", "--user", userId, packageName)
            }
        }

        PM_ENABLE -> {
            val packageName = args.getOrNull(0) ?: return null
            val userId = optionalUserId(args, index = 1) ?: return null
            if (args.size > 2) return null
            if (!isSafePackageName(packageName)) {
                null
            } else {
                listOf("pm", "enable", "--user", userId, packageName)
            }
        }

        PM_ENABLE_COMPONENT -> buildComponentEnableDisableArgv(args, enable = true)

        PM_DISABLE_COMPONENT -> buildComponentEnableDisableArgv(args, enable = false)

        PM_LIST_USERS -> {
            if (args.isNotEmpty()) null else listOf("pm", "list", "users")
        }

        PM_INSTALL_EXISTING -> {
            val packageName = args.getOrNull(0) ?: return null
            val userId = optionalUserId(args, index = 1) ?: return null
            if (args.size > 2) return null
            if (!isSafePackageName(packageName)) {
                null
            } else {
                listOf("pm", "install-existing", "--user", userId, packageName)
            }
        }

        PM_SUSPEND -> {
            // HyperOS may apply suspend only for the calling user; shell failure is surfaced to UI.
            val packageName = args.getOrNull(0) ?: return null
            val userId = optionalUserId(args, index = 1) ?: return null
            if (args.size > 2) return null
            if (!isSafePackageName(packageName)) {
                null
            } else {
                listOf("pm", "suspend", "--user", userId, packageName)
            }
        }

        PM_UNSUSPEND -> {
            val packageName = args.getOrNull(0) ?: return null
            val userId = optionalUserId(args, index = 1) ?: return null
            if (args.size > 2) return null
            if (!isSafePackageName(packageName)) {
                null
            } else {
                listOf("pm", "unsuspend", "--user", userId, packageName)
            }
        }

        PM_REVOKE -> {
            val packageName = args.getOrNull(0) ?: return null
            val permission = args.getOrNull(1) ?: return null
            if (args.size != 2) return null
            if (!isSafePackageName(packageName) || !isSafePermissionName(permission)) {
                null
            } else {
                listOf("pm", "revoke", packageName, permission)
            }
        }

        APPOPS_SET -> {
            val packageName = args.getOrNull(0) ?: return null
            val op = args.getOrNull(1) ?: return null
            val mode = args.getOrNull(2) ?: return null
            if (args.size != 3) return null
            if (!isSafePackageName(packageName) || !isAllowedAppOp(op) || !isAllowedAppOpMode(mode)) {
                null
            } else {
                listOf("appops", "set", packageName, op, mode)
            }
        }

        APPOPS_GET -> {
            val packageName = args.getOrNull(0) ?: return null
            val op = args.getOrNull(1) ?: return null
            if (args.size != 2) return null
            if (!isSafePackageName(packageName) || !isAllowedAppOp(op)) {
                null
            } else {
                listOf("appops", "get", packageName, op)
            }
        }

        DEVICEIDLE_WHITELIST -> {
            val action = args.getOrNull(0) ?: return null
            val packageName = args.getOrNull(1) ?: return null
            if (args.size != 2) return null
            if (action !in DEVICEIDLE_ACTIONS || !isSafePackageName(packageName)) {
                null
            } else {
                // Single token "+pkg" / "-pkg" / "=pkg" — never concatenate user strings into a shell.
                listOf("dumpsys", "deviceidle", "whitelist", action + packageName)
            }
        }

        LS_ANDROID_ROOT -> {
            val root = resolveRoot(args.singleOrNull()) ?: return null
            listOf("ls", "-1", root.absolutePath)
        }

        DU_ANDROID_PKG -> {
            val path = androidPackagePath(args.getOrNull(0), args.getOrNull(1)) ?: return null
            if (args.size != 2) return null
            listOf("du", "-s", path)
        }

        RM_ANDROID_PKG -> {
            val path = androidPackagePath(args.getOrNull(0), args.getOrNull(1)) ?: return null
            if (args.size != 2) return null
            // ProcessBuilder argv — never a concatenated shell string.
            listOf("rm", "-rf", path)
        }

        TAR_ANDROID_PKG_OUT -> {
            if (args.size != 3) return null
            val root = resolveRoot(args[0]) ?: return null
            val packageName = args[1]
            val destTar = args[2]
            if (!isSafePackageName(packageName) || !isSafeStagingTarPath(destTar)) return null
            // tar -cf dest -C /storage/.../Android/data packageName
            listOf("tar", "-cf", destTar, "-C", root.absolutePath, packageName)
        }

        TAR_ANDROID_PKG_IN -> {
            if (args.size != 3) return null
            val root = resolveRoot(args[0]) ?: return null
            val packageName = args[1]
            val srcTar = args[2]
            if (!isSafePackageName(packageName) || !isSafeStagingTarPath(srcTar)) return null
            listOf("tar", "-xf", srcTar, "-C", root.absolutePath)
        }

        AM_FORCE_STOP -> {
            val packageName = args.singleOrNull() ?: return null
            if (!isSafePackageName(packageName)) return null
            listOf("am", "force-stop", packageName)
        }

        DUMPSYS_MEMINFO -> {
            if (args.isNotEmpty()) null else listOf("dumpsys", "meminfo")
        }

        DUMPSYS_CPUINFO -> {
            if (args.isNotEmpty()) null else listOf("dumpsys", "cpuinfo")
        }

        DUMPSYS_BATTERY -> {
            if (args.isNotEmpty()) null else listOf("dumpsys", "battery")
        }

        DUMPSYS_NETSTATS -> {
            // Full dump: HyperOS --checkin is wifi-bucket only; UID table lives in the full dump.
            // Output is truncated in ShellRunner (line/byte caps).
            if (args.isNotEmpty()) null else listOf("dumpsys", "netstats")
        }

        DUMPSYS_THERMALSERVICE -> {
            if (args.isNotEmpty()) null else listOf("dumpsys", "thermalservice")
        }

        TOP_ONCE -> {
            // toybox: -n 1 one iteration, -m 20 max rows (no pipes / free text).
            if (args.isNotEmpty()) null else listOf("top", "-n", "1", "-m", "20")
        }

        else -> null
    }

    fun resolveRoot(pathKey: String?): AndroidSharedRoot? = pathKey?.let { AndroidSharedRoot.fromPathKey(it) }

    fun androidPackagePath(pathKey: String?, packageName: String?): String? {
        val root = resolveRoot(pathKey) ?: return null
        if (packageName == null || !isSafePackageName(packageName)) return null
        return "${root.absolutePath}/$packageName"
    }

    fun isSafePackageName(value: String): Boolean = value.length in 3..255 && PACKAGE_NAME.matches(value)

    /** Digits-only Android user id for `--user` (0…99999). */
    fun isSafeUserId(value: String): Boolean = USER_ID.matches(value) && value.toIntOrNull()?.let { it in 0..99_999 } == true

    fun isSafeComponentClassName(value: String): Boolean {
        if (value.length !in 1..255) return false
        if (value.contains("..") || value.contains('/') || value.contains('\\')) return false
        return COMPONENT_CLASS.matches(value)
    }

    fun isSafePermissionName(value: String): Boolean = value.length in 8..255 && PERMISSION_NAME.matches(value)

    fun isAllowedAppOp(value: String): Boolean = value in ALLOWED_APP_OPS

    fun isAllowedAppOpMode(value: String): Boolean = value in ALLOWED_APP_OP_MODES

    fun isSafeApkPath(value: String): Boolean {
        if (value.length !in 6..4096) return false
        if (value.contains('\u0000') || value.contains('`') || value.contains('$') ||
            value.contains('|') || value.contains(';') || value.contains('&') ||
            value.contains('\n') || value.contains('\r') || value.contains('"') ||
            value.contains('\'') || value.contains(' ')
        ) {
            return false
        }
        if (value.contains("..")) return false
        return APK_PATH.matches(value)
    }

    fun isSafeStagingTarPath(value: String): Boolean {
        if (value.length !in 20..4096) return false
        if (value.contains('\u0000') || value.contains('`') || value.contains('$') ||
            value.contains('|') || value.contains(';') || value.contains('&') ||
            value.contains('\n') || value.contains('\r') || value.contains('"') ||
            value.contains('\'') || value.contains(' ')
        ) {
            return false
        }
        if (value.contains("..")) return false
        return STAGING_TAR_PATH.matches(value)
    }

    /** Cap split count so argv stays bounded for UserService. */
    private const val MAX_INSTALL_MULTIPLE = 64

    /**
     * Builds `pm enable|disable --user N package/class` from validated tokens.
     * Optional trailing user id (M56); missing → `"0"`. Never concatenates free text.
     */
    private fun buildComponentEnableDisableArgv(args: List<String>, enable: Boolean): List<String>? {
        if (args.size !in 2..3) return null
        val packageName = args[0]
        val className = args[1]
        val userId = optionalUserId(args, index = 2) ?: return null
        if (!isSafePackageName(packageName) || !isSafeComponentClassName(className)) return null
        val component = "$packageName/$className"
        return listOf("pm", if (enable) "enable" else "disable", "--user", userId, component)
    }

    private fun buildInstallMultipleArgv(args: List<String>, downgrade: Boolean): List<String>? {
        if (args.isEmpty()) return null
        val last = args.last()
        val (paths, userId) = if (args.size >= 2 && isSafeUserId(last) && !isSafeApkPath(last)) {
            args.dropLast(1) to last
        } else {
            args to "0"
        }
        if (paths.isEmpty() || paths.size > MAX_INSTALL_MULTIPLE) return null
        if (paths.any { !isSafeApkPath(it) }) return null
        return buildList {
            add("pm")
            add("install-multiple")
            add("-r")
            if (downgrade) add("-d")
            add("--user")
            add(userId)
            addAll(paths)
        }
    }

    /**
     * Optional trailing `--user` id. Missing arg → `"0"`.
     * Present but invalid → null (reject the whole command).
     */
    private fun optionalUserId(args: List<String>, index: Int): String? {
        val raw = args.getOrNull(index) ?: return "0"
        return if (isSafeUserId(raw)) raw else null
    }
}

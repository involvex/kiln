package dev.involvex.kiln.core.privilege.shell

import dev.involvex.kiln.core.model.AndroidSharedRoot
import dev.involvex.kiln.core.model.PermissionAppOpsAllowlist

/**
 * Fixed allowlist for privileged shell invocations.
 * Arguments are validated; never concatenate user input into a shell string.
 */
object ShellAllowlist {
    const val ID = "id"
    const val PM_INSTALL = "pm_install"
    const val PM_INSTALL_DOWNGRADE = "pm_install_downgrade"
    const val PM_UNINSTALL = "pm_uninstall"
    const val PM_CLEAR_CACHE = "pm_clear_cache"
    const val PM_TRIM_CACHES = "pm_trim_caches"
    const val PM_DISABLE_USER = "pm_disable_user"
    const val PM_ENABLE = "pm_enable"
    const val PM_INSTALL_EXISTING = "pm_install_existing"
    const val PM_REVOKE = "pm_revoke"
    const val APPOPS_SET = "appops_set"
    const val LS_ANDROID_ROOT = "ls_android_root"
    const val DU_ANDROID_PKG = "du_android_pkg"
    const val RM_ANDROID_PKG = "rm_android_pkg"

    private val PACKAGE_NAME = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$")
    private val PERMISSION_NAME = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$")
    private val APK_PATH = Regex("^/[\\w./\\-+]+\\.apk$", RegexOption.IGNORE_CASE)

    /** Desired free-space argument for `pm trim-caches` (digits only, capped length). */
    private val TRIM_BYTES = Regex("^[1-9][0-9]{0,18}$")

    val ALLOWED_APP_OPS: Set<String> get() = PermissionAppOpsAllowlist.OPS

    val ALLOWED_APP_OP_MODES: Set<String> get() = PermissionAppOpsAllowlist.MODES

    /**
     * Builds argv for [ProcessBuilder], or null if [commandId]/[args] are rejected.
     */
    fun buildArgv(commandId: String, args: List<String>): List<String>? = when (commandId) {
        ID -> {
            if (args.isNotEmpty()) null else listOf("id")
        }

        PM_INSTALL -> {
            val path = args.singleOrNull() ?: return null
            if (!isSafeApkPath(path)) null else listOf("pm", "install", "-r", "--user", "0", path)
        }

        PM_INSTALL_DOWNGRADE -> {
            val path = args.singleOrNull() ?: return null
            // -d allows installing an older versionCode over a newer one (rollback).
            if (!isSafeApkPath(path)) null else listOf("pm", "install", "-r", "-d", "--user", "0", path)
        }

        PM_UNINSTALL -> {
            val packageName = args.getOrNull(0) ?: return null
            val keepData = args.getOrNull(1) == "keep"
            if (!isSafePackageName(packageName)) {
                null
            } else if (keepData) {
                listOf("pm", "uninstall", "-k", "--user", "0", packageName)
            } else {
                listOf("pm", "uninstall", "--user", "0", packageName)
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
            val packageName = args.singleOrNull() ?: return null
            if (!isSafePackageName(packageName)) {
                null
            } else {
                listOf("pm", "disable-user", "--user", "0", packageName)
            }
        }

        PM_ENABLE -> {
            val packageName = args.singleOrNull() ?: return null
            if (!isSafePackageName(packageName)) {
                null
            } else {
                listOf("pm", "enable", "--user", "0", packageName)
            }
        }

        PM_INSTALL_EXISTING -> {
            val packageName = args.singleOrNull() ?: return null
            if (!isSafePackageName(packageName)) {
                null
            } else {
                listOf("pm", "install-existing", "--user", "0", packageName)
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

        else -> null
    }

    fun resolveRoot(pathKey: String?): AndroidSharedRoot? = pathKey?.let { AndroidSharedRoot.fromPathKey(it) }

    fun androidPackagePath(pathKey: String?, packageName: String?): String? {
        val root = resolveRoot(pathKey) ?: return null
        if (packageName == null || !isSafePackageName(packageName)) return null
        return "${root.absolutePath}/$packageName"
    }

    fun isSafePackageName(value: String): Boolean = value.length in 3..255 && PACKAGE_NAME.matches(value)

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
}

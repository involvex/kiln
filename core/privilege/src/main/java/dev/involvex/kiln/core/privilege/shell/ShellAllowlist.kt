package dev.involvex.kiln.core.privilege.shell

/**
 * Fixed allowlist for privileged shell invocations.
 * Arguments are validated; never concatenate user input into a shell string.
 */
object ShellAllowlist {
    const val ID = "id"
    const val PM_INSTALL = "pm_install"
    const val PM_UNINSTALL = "pm_uninstall"
    const val PM_CLEAR_CACHE = "pm_clear_cache"
    const val PM_TRIM_CACHES = "pm_trim_caches"

    private val PACKAGE_NAME = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$")
    private val APK_PATH = Regex("^/[\\w./\\-+]+\\.apk$", RegexOption.IGNORE_CASE)

    /** Desired free-space argument for `pm trim-caches` (digits only, capped length). */
    private val TRIM_BYTES = Regex("^[1-9][0-9]{0,18}$")

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

        else -> null
    }

    fun isSafePackageName(value: String): Boolean = value.length in 3..255 && PACKAGE_NAME.matches(value)

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

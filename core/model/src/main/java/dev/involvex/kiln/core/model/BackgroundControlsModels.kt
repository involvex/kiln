package dev.involvex.kiln.core.model

/**
 * AOSP battery usage tristate (Android 12+ App battery usage).
 * Mapped via deviceidle whitelist + [BackgroundControlsOps.RUN_ANY_IN_BACKGROUND].
 */
enum class BatteryUsageMode {
    UNRESTRICTED,
    OPTIMIZED,
    RESTRICTED,
}

data class BackgroundControlsApp(
    val packageName: String,
    val label: String,
    val isSystem: Boolean,
    val autostartAllowed: Boolean?,
    val batteryMode: BatteryUsageMode?,
)

object BackgroundControlsOps {
    /** Xiaomi / HyperOS AUTO_START. */
    const val AUTO_START = "10008"

    /** Xiaomi / HyperOS BACKGROUND_START_ACTIVITY (companion). */
    const val BACKGROUND_START_ACTIVITY = "10021"

    const val RUN_ANY_IN_BACKGROUND = "RUN_ANY_IN_BACKGROUND"

    val AUTOSTART_OPS: Set<String> = setOf(AUTO_START, BACKGROUND_START_ACTIVITY)

    val ALL_OPS: Set<String> = AUTOSTART_OPS + RUN_ANY_IN_BACKGROUND

    val MODES: Set<String> = setOf("allow", "ignore", "default")
}

object BackgroundControlsGate {
    fun canMutate(capabilities: Set<PrivilegeCapability>): Boolean = PrivilegeCapability.MANAGE_APP_OPS in capabilities
}

/**
 * Pure mapping for battery UI ↔ shell state.
 * [whitelisted] = result of `dumpsys deviceidle whitelist =pkg`.
 * [runAnyMode] = parsed AppOp mode for RUN_ANY_IN_BACKGROUND (`allow` / `ignore` / other).
 */
object BatteryUsageModeMapper {
    fun fromShell(whitelisted: Boolean, runAnyMode: String): BatteryUsageMode = when {
        whitelisted -> BatteryUsageMode.UNRESTRICTED
        runAnyMode.equals("ignore", ignoreCase = true) -> BatteryUsageMode.RESTRICTED
        else -> BatteryUsageMode.OPTIMIZED
    }

    /** Returns Pair(deviceIdleAction `+`/`-`, runAny mode). */
    fun toShell(mode: BatteryUsageMode): Pair<String, String> = when (mode) {
        BatteryUsageMode.UNRESTRICTED -> "+" to "allow"
        BatteryUsageMode.OPTIMIZED -> "-" to "allow"
        BatteryUsageMode.RESTRICTED -> "-" to "ignore"
    }
}

/** Parse `appops get` stdout for allow/ignore/default. */
object AppOpOutputParser {
    private val MODE_TOKEN = Regex("""mode\s*[:=]\s*(allow|ignore|default)""")

    fun parseMode(raw: String): String? {
        val lower = raw.lowercase()
        MODE_TOKEN.find(lower)?.groupValues?.getOrNull(1)?.let { return it }
        return when {
            "ignore" in lower -> "ignore"
            "allow" in lower -> "allow"
            "default" in lower -> "default"
            else -> null
        }
    }

    /** Strict allow (e.g. RUN_ANY_IN_BACKGROUND). */
    fun isAllowed(raw: String): Boolean = parseMode(raw) == "allow"

    /**
     * HyperOS Autostart Settings treats anything other than explicit `ignore` as enabled
     * (including `default`). Matching that avoids Kiln showing Off while Settings shows On.
     */
    fun isAutostartAllowed(raw: String): Boolean {
        val mode = parseMode(raw) ?: return false
        return !mode.equals("ignore", ignoreCase = true)
    }
}

/** List filter for Autostart state on Background controls. */
enum class AutostartListFilter {
    ALL,
    ENABLED,
    DISABLED,
}

/** Parse `dumpsys deviceidle whitelist =pkg` — true when package is on the allowlist. */
object DeviceIdleWhitelistParser {
    fun isWhitelisted(packageName: String, raw: String): Boolean {
        val trimmed = raw.trim()
        if (trimmed.equals("true", ignoreCase = true)) return true
        if (trimmed.equals("false", ignoreCase = true)) return false
        return trimmed.lineSequence().any { line ->
            line.contains(packageName, ignoreCase = false) &&
                !line.trimStart().startsWith("ERR:")
        }
    }
}

package dev.involvex.kiln.core.model

/** One installed app with dangerous permissions that are currently granted. */
data class PermissionAuditApp(
    val packageName: String,
    val label: String,
    val isSystemApp: Boolean,
    val dangerousGranted: List<AppPermissionEntry>,
) {
    val dangerousGrantedCount: Int get() = dangerousGranted.size
}

/** Pure helpers for overview filtering (unit-tested). */
object PermissionAuditFilter {
    fun withDangerousGranted(apps: List<PermissionAuditApp>): List<PermissionAuditApp> = apps.filter { it.dangerousGranted.isNotEmpty() }

    fun filter(apps: List<PermissionAuditApp>, query: String): List<PermissionAuditApp> {
        val needle = query.trim().lowercase()
        if (needle.isEmpty()) return apps
        return apps.filter { app ->
            app.label.lowercase().contains(needle) ||
                app.packageName.lowercase().contains(needle) ||
                app.dangerousGranted.any { it.name.lowercase().contains(needle) }
        }
    }

    fun countDangerousGranted(apps: List<PermissionAuditApp>): Int = apps.sumOf { it.dangerousGrantedCount }
}

/** Gate for mutate actions (unit-tested). */
object PermissionAuditGate {
    fun canMutate(capabilities: Set<PrivilegeCapability>): Boolean = PrivilegeCapability.MANAGE_APP_OPS in capabilities
}

/**
 * Fixed AppOps CLI surface for M27 (mirrored by ShellAllowlist).
 * Not the full AppOpsManager catalog.
 */
object PermissionAppOpsAllowlist {
    val OPS: Set<String> = setOf(
        "CAMERA",
        "RECORD_AUDIO",
        "FINE_LOCATION",
        "COARSE_LOCATION",
        "READ_CONTACTS",
        "WRITE_CONTACTS",
        "READ_EXTERNAL_STORAGE",
        "WRITE_EXTERNAL_STORAGE",
        "GET_ACCOUNTS",
    )

    val MODES: Set<String> = setOf("allow", "ignore", "default")
}

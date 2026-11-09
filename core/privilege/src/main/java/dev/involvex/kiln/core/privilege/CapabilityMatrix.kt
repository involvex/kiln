package dev.involvex.kiln.core.privilege

import dev.involvex.kiln.core.model.CapabilityAvailability
import dev.involvex.kiln.core.model.PrivilegeCapability
import dev.involvex.kiln.core.model.PrivilegeTier

/**
 * Derives the capability matrix from self-test UID (PLAN §4.9 D).
 * Shell uid 2000 = ADB; uid 0 = root.
 */
object CapabilityMatrix {
    fun fromUid(uid: Int, tier: PrivilegeTier): Map<PrivilegeCapability, CapabilityAvailability> {
        if (tier == PrivilegeTier.NONE) {
            return PrivilegeCapability.entries.associateWith { CapabilityAvailability.UNAVAILABLE }
        }
        val shell = uid == SHELL_UID || uid == ROOT_UID
        val root = uid == ROOT_UID
        return mapOf(
            PrivilegeCapability.SILENT_INSTALL to avail(shell),
            PrivilegeCapability.SILENT_UNINSTALL to avail(shell),
            PrivilegeCapability.CLEAR_CACHE to avail(shell),
            PrivilegeCapability.RUN_SHELL_DIAGNOSTIC to avail(shell),
            PrivilegeCapability.ACCESS_ANDROID_DATA to avail(shell),
            PrivilegeCapability.DEBLOAT to avail(shell),
            PrivilegeCapability.MANAGE_APP_OPS to avail(shell),
            PrivilegeCapability.FULL_BACKUP to if (root) {
                CapabilityAvailability.AVAILABLE
            } else {
                CapabilityAvailability.RESTRICTED
            },
        )
    }

    fun capabilities(uid: Int, tier: PrivilegeTier): Set<PrivilegeCapability> = fromUid(uid, tier)
        .filterValues { it == CapabilityAvailability.AVAILABLE }
        .keys

    private fun avail(ok: Boolean): CapabilityAvailability =
        if (ok) CapabilityAvailability.AVAILABLE else CapabilityAvailability.UNAVAILABLE

    const val SHELL_UID = 2000
    const val ROOT_UID = 0
}

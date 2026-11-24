package dev.involvex.kiln.core.model

/**
 * Pure gate for M55 dual-user install/uninstall UX.
 * Single-user devices never show a picker.
 */
object DualUserInstallGate {
    fun showPicker(users: List<AndroidUserInfo>, hasSilentCapability: Boolean): Boolean = hasSilentCapability && users.size >= 2

    /**
     * Picks a target user id from the census. Unknown/missing selection falls back to [currentUserId].
     */
    fun resolveTargetUserId(selectedUserId: Int?, users: List<AndroidUserInfo>, currentUserId: Int): Int {
        val candidate = selectedUserId ?: currentUserId
        if (users.isEmpty()) return currentUserId
        return if (users.any { it.id == candidate }) candidate else currentUserId
    }
}

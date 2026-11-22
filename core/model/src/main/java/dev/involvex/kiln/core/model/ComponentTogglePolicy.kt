package dev.involvex.kiln.core.model

/**
 * Pure gates for App Detail component enable/disable (M51).
 * Shell argv validation lives in ShellAllowlist; this gates UI + feature calls.
 */
object ComponentTogglePolicy {
    /**
     * Toggle is offered only for exported components when privilege is present
     * and the package is not on the protected denylist.
     */
    fun canOfferToggle(exported: Boolean, hasDebloatCapability: Boolean, packageName: String, denylistedPackages: Set<String>): Boolean {
        if (!exported || !hasDebloatCapability) return false
        if (packageName in denylistedPackages) return false
        if (ProtectedPackages.isHardBlocked(packageName)) return false
        return true
    }

    fun isDenylisted(packageName: String, denylistedPackages: Set<String>): Boolean =
        packageName in denylistedPackages || ProtectedPackages.isHardBlocked(packageName)
}

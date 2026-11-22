package dev.involvex.kiln.feature.debloat.denylist

import dev.involvex.kiln.core.model.ProtectedPackages

/**
 * Hard denylist packages that must never be selectable for debloat actions (PLAN §4.10).
 * Dynamic roles (launcher/IME/dialer/SMS) are merged by [DebloatDenylistResolver].
 * Canonical set lives in [ProtectedPackages] (shared with component toggles).
 */
object DebloatDenylist {
    const val REASON_HARD = ProtectedPackages.REASON_HARD
    const val REASON_SELF = ProtectedPackages.REASON_SELF
    const val REASON_LAUNCHER = ProtectedPackages.REASON_LAUNCHER
    const val REASON_IME = ProtectedPackages.REASON_IME
    const val REASON_DIALER = ProtectedPackages.REASON_DIALER
    const val REASON_SMS = ProtectedPackages.REASON_SMS

    val HARD_PACKAGES: Set<String> get() = ProtectedPackages.HARD_PACKAGES

    fun isHardBlocked(packageName: String): Boolean = ProtectedPackages.isHardBlocked(packageName)
}

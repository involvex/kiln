package dev.involvex.kiln.feature.debloat.policy

import dev.involvex.kiln.core.model.DebloatAction
import dev.involvex.kiln.core.model.DebloatAppRow
import dev.involvex.kiln.core.model.DebloatPackageState
import dev.involvex.kiln.core.model.DebloatRating

/**
 * Pure selection rules (PLAN §4.10). Unit-tested; no Android deps.
 */
object DebloatPolicy {
    fun canSelect(row: DebloatAppRow, action: DebloatAction, expertMode: Boolean): Boolean {
        if (row.denylisted) return false
        if (row.rating == DebloatRating.UNSAFE) return false
        if (row.rating == DebloatRating.EXPERT && !expertMode) return false
        if (row.rating == DebloatRating.UNKNOWN && !expertMode) return false
        return when (action) {
            DebloatAction.DISABLE -> row.state == DebloatPackageState.ENABLED

            // UI “restore” chip uses ENABLE; also allow selecting uninstalled rows for install-existing.
            DebloatAction.ENABLE ->
                row.state == DebloatPackageState.DISABLED ||
                    row.state == DebloatPackageState.UNINSTALLED_FOR_USER

            DebloatAction.UNINSTALL_FOR_USER ->
                row.state == DebloatPackageState.ENABLED || row.state == DebloatPackageState.DISABLED

            DebloatAction.INSTALL_EXISTING -> row.state == DebloatPackageState.UNINSTALLED_FOR_USER
        }
    }

    fun defaultActionFor(state: DebloatPackageState): DebloatAction? = when (state) {
        DebloatPackageState.ENABLED -> DebloatAction.DISABLE
        DebloatPackageState.DISABLED -> DebloatAction.ENABLE
        DebloatPackageState.UNINSTALLED_FOR_USER -> DebloatAction.INSTALL_EXISTING
    }
}

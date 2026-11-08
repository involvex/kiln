package dev.involvex.kiln.feature.debloat

import dev.involvex.kiln.core.model.DebloatAction
import dev.involvex.kiln.core.model.DebloatActionResult
import dev.involvex.kiln.core.model.DebloatAppRow
import dev.involvex.kiln.core.model.DebloatPackageState
import dev.involvex.kiln.core.model.DebloatRating

enum class DebloatFilterState {
    ALL,
    ENABLED,
    DISABLED,
    UNINSTALLED,
}

data class DebloatUiState(
    val hasDebloatCapability: Boolean = false,
    val isLoading: Boolean = false,
    val rows: List<DebloatAppRow> = emptyList(),
    val query: String = "",
    val filter: DebloatFilterState = DebloatFilterState.ALL,
    val systemOnly: Boolean = true,
    val expertMode: Boolean = false,
    val selected: Set<String> = emptySet(),
    val batchAction: DebloatAction = DebloatAction.DISABLE,
    val snapshotAtEpochMs: Long? = null,
    val pendingConfirm: Boolean = false,
    val confirmText: String = "",
    val running: Boolean = false,
    val lastResults: List<DebloatActionResult> = emptyList(),
    val message: String? = null,
) {
    val visibleRows: List<DebloatAppRow>
        get() {
            val q = query.trim()
            return rows.asSequence()
                .filter { !systemOnly || it.isSystem }
                .filter {
                    when (filter) {
                        DebloatFilterState.ALL -> true
                        DebloatFilterState.ENABLED -> it.state == DebloatPackageState.ENABLED
                        DebloatFilterState.DISABLED -> it.state == DebloatPackageState.DISABLED
                        DebloatFilterState.UNINSTALLED -> it.state == DebloatPackageState.UNINSTALLED_FOR_USER
                    }
                }
                .filter {
                    q.isEmpty() ||
                        it.label.contains(q, ignoreCase = true) ||
                        it.packageName.contains(q, ignoreCase = true)
                }
                .toList()
        }

    val selectedRows: List<DebloatAppRow>
        get() = rows.filter { it.packageName in selected }

    val needsTypedConfirm: Boolean
        get() = batchAction == DebloatAction.UNINSTALL_FOR_USER ||
            selectedRows.any {
                it.rating == DebloatRating.UNKNOWN || it.rating == DebloatRating.EXPERT
            }
}

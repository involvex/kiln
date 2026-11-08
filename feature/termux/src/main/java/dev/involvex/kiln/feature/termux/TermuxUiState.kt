package dev.involvex.kiln.feature.termux

import dev.involvex.kiln.core.model.TermuxPrefs
import dev.involvex.kiln.feature.termux.run.TermuxCommandResult

enum class TermuxStatusKind {
    None,
    Disabled,
    NotInstalled,
    PermissionDenied,
    StartFailed,
    Running,
    Result,
}

data class TermuxUiState(
    val title: String = "",
    val prefs: TermuxPrefs = TermuxPrefs(),
    val packageIdDraft: String = TermuxPrefs.DEFAULT_TERMUX_PACKAGE_ID,
    val packageInstalled: Boolean = false,
    val permissionGranted: Boolean = false,
    val isRunning: Boolean = false,
    val pendingExecutionId: Int? = null,
    val lastResult: TermuxCommandResult? = null,
    val statusKind: TermuxStatusKind = TermuxStatusKind.None,
    val startFailedDetail: String? = null,
)

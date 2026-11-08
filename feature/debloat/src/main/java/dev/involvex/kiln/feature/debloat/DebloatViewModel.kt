package dev.involvex.kiln.feature.debloat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.core.model.DebloatAction
import dev.involvex.kiln.core.model.DebloatPackageState
import dev.involvex.kiln.core.model.PrivilegeCapability
import dev.involvex.kiln.core.model.PrivilegedExecutor
import dev.involvex.kiln.feature.debloat.data.DebloatRepository
import dev.involvex.kiln.feature.debloat.policy.DebloatPolicy
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class DebloatViewModel @Inject constructor(private val repository: DebloatRepository, private val privilegedExecutor: PrivilegedExecutor) :
    ViewModel() {
    private val _uiState = MutableStateFlow(DebloatUiState())
    val uiState: StateFlow<DebloatUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            privilegedExecutor.observeStatus().collect { status ->
                _uiState.update {
                    it.copy(hasDebloatCapability = PrivilegeCapability.DEBLOAT in status.capabilities)
                }
            }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            val rows = repository.loadRows()
            val snapshot = repository.currentSnapshot()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    rows = rows,
                    snapshotAtEpochMs = snapshot?.createdAtEpochMs,
                    selected = it.selected.intersect(rows.map { row -> row.packageName }.toSet()),
                )
            }
        }
    }

    fun setQuery(query: String) = _uiState.update { it.copy(query = query) }

    fun setFilter(filter: DebloatFilterState) = _uiState.update { it.copy(filter = filter) }

    fun setSystemOnly(systemOnly: Boolean) = _uiState.update { it.copy(systemOnly = systemOnly) }

    fun setExpertMode(enabled: Boolean) {
        _uiState.update { state ->
            val next = state.copy(expertMode = enabled)
            next.copy(selected = next.selected.filter { pkg -> selectable(pkg, next) }.toSet())
        }
    }

    fun setBatchAction(action: DebloatAction) {
        _uiState.update { state ->
            val next = state.copy(batchAction = action)
            next.copy(selected = next.selected.filter { pkg -> selectable(pkg, next) }.toSet())
        }
    }

    fun toggle(packageName: String) {
        _uiState.update { state ->
            if (!selectable(packageName, state)) return@update state
            val next = state.selected.toMutableSet()
            if (!next.add(packageName)) next.remove(packageName)
            state.copy(selected = next)
        }
    }

    fun requestApply() {
        if (_uiState.value.selected.isEmpty()) return
        _uiState.update { it.copy(pendingConfirm = true, confirmText = "") }
    }

    fun dismissConfirm() = _uiState.update { it.copy(pendingConfirm = false, confirmText = "") }

    fun setConfirmText(text: String) = _uiState.update { it.copy(confirmText = text) }

    fun confirmApply() {
        val state = _uiState.value
        if (state.needsTypedConfirm && !isTypedConfirmValid(state.confirmText)) return
        val targets = state.selectedRows.mapNotNull { row ->
            val action = when (state.batchAction) {
                DebloatAction.ENABLE -> when (row.state) {
                    DebloatPackageState.DISABLED -> DebloatAction.ENABLE
                    DebloatPackageState.UNINSTALLED_FOR_USER -> DebloatAction.INSTALL_EXISTING
                    else -> null
                }

                else -> state.batchAction
            } ?: return@mapNotNull null
            row.packageName to action
        }
        viewModelScope.launch {
            _uiState.update { it.copy(running = true, pendingConfirm = false, message = null) }
            val results = repository.applyActions(targets)
            val ok = results.count { it.success }
            val fail = results.size - ok
            _uiState.update {
                it.copy(
                    running = false,
                    selected = emptySet(),
                    lastResults = results,
                    message = "done:$ok:$fail",
                )
            }
            refresh()
        }
    }

    fun createSnapshot() {
        viewModelScope.launch {
            when (val result = repository.ensureSnapshot()) {
                is Result.Failure -> _uiState.update { it.copy(message = result.message) }

                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            snapshotAtEpochMs = result.value.createdAtEpochMs,
                            message = "snapshot_ok",
                        )
                    }
                }
            }
        }
    }

    fun restoreSnapshot() {
        viewModelScope.launch {
            _uiState.update { it.copy(running = true, message = null) }
            val results = repository.restoreSnapshot()
            val ok = results.count { it.success }
            val fail = results.size - ok
            _uiState.update {
                it.copy(
                    running = false,
                    lastResults = results,
                    message = "restore:$ok:$fail",
                )
            }
            refresh()
        }
    }

    fun consumeMessage() = _uiState.update { it.copy(message = null) }

    private fun selectable(packageName: String, state: DebloatUiState): Boolean {
        val row = state.rows.find { it.packageName == packageName } ?: return false
        return DebloatPolicy.canSelect(row, state.batchAction, state.expertMode)
    }

    companion object {
        fun isTypedConfirmValid(text: String): Boolean {
            val normalized = text.trim().uppercase()
            return normalized == "I UNDERSTAND" || normalized == "ICH VERSTEHE"
        }
    }
}

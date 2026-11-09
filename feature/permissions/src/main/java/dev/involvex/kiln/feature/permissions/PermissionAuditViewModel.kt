package dev.involvex.kiln.feature.permissions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.involvex.kiln.core.model.PermissionAppOpsAllowlist
import dev.involvex.kiln.core.model.PermissionAuditApp
import dev.involvex.kiln.core.model.PermissionAuditFilter
import dev.involvex.kiln.core.model.PermissionAuditGate
import dev.involvex.kiln.core.model.PrivilegedExecutor
import dev.involvex.kiln.feature.permissions.data.PermissionAuditRepository
import dev.involvex.kiln.feature.permissions.data.PermissionMutationTarget
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PendingRevoke(val targets: List<PermissionMutationTarget>)

data class PendingAppOp(val packageName: String, val op: String, val mode: String)

data class PermissionAuditUiState(
    val isLoading: Boolean = false,
    val query: String = "",
    val apps: List<PermissionAuditApp> = emptyList(),
    val selectedPackage: String? = null,
    val selectedPermissions: Set<String> = emptySet(),
    val canMutate: Boolean = false,
    val selectedAppOp: String = PermissionAppOpsAllowlist.OPS.first(),
    val selectedAppOpMode: String = "ignore",
    val pendingRevoke: PendingRevoke? = null,
    val pendingAppOp: PendingAppOp? = null,
    val running: Boolean = false,
    val message: String? = null,
) {
    val filteredApps: List<PermissionAuditApp>
        get() = PermissionAuditFilter.filter(apps, query)

    val detailApp: PermissionAuditApp?
        get() = apps.firstOrNull { it.packageName == selectedPackage }

    val allowedAppOps: List<String> get() = PermissionAppOpsAllowlist.OPS.sorted()

    val allowedModes: List<String> get() = PermissionAppOpsAllowlist.MODES.sorted()
}

@HiltViewModel
class PermissionAuditViewModel @Inject constructor(
    private val repository: PermissionAuditRepository,
    private val privilegedExecutor: PrivilegedExecutor,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PermissionAuditUiState())
    val uiState: StateFlow<PermissionAuditUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            privilegedExecutor.observeStatus().collect { status ->
                _uiState.update {
                    it.copy(canMutate = PermissionAuditGate.canMutate(status.capabilities))
                }
            }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            val apps = repository.loadApps()
            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    apps = apps,
                    selectedPackage = state.selectedPackage?.takeIf { pkg ->
                        apps.any { it.packageName == pkg }
                    },
                    selectedPermissions = emptySet(),
                )
            }
        }
    }

    fun setQuery(query: String) = _uiState.update { it.copy(query = query) }

    fun openDetail(packageName: String) =
        _uiState.update { it.copy(selectedPackage = packageName, selectedPermissions = emptySet(), message = null) }

    fun closeDetail() = _uiState.update { it.copy(selectedPackage = null, selectedPermissions = emptySet()) }

    fun togglePermission(permission: String) {
        _uiState.update { state ->
            val next = state.selectedPermissions.toMutableSet()
            if (!next.add(permission)) next.remove(permission)
            state.copy(selectedPermissions = next)
        }
    }

    fun selectAllDangerousForDetail() {
        val app = _uiState.value.detailApp ?: return
        _uiState.update {
            it.copy(selectedPermissions = app.dangerousGranted.map { entry -> entry.name }.toSet())
        }
    }

    fun setAppOpSelection(op: String) {
        if (op in PermissionAppOpsAllowlist.OPS) {
            _uiState.update { it.copy(selectedAppOp = op) }
        }
    }

    fun setAppOpMode(mode: String) {
        if (mode in PermissionAppOpsAllowlist.MODES) {
            _uiState.update { it.copy(selectedAppOpMode = mode) }
        }
    }

    fun requestRevokeSelected() {
        val state = _uiState.value
        val pkg = state.selectedPackage ?: return
        if (state.selectedPermissions.isEmpty()) return
        if (!state.canMutate) {
            _uiState.update { it.copy(message = "capability_missing") }
            return
        }
        val targets = state.selectedPermissions.map { PermissionMutationTarget(pkg, it) }
        _uiState.update { it.copy(pendingRevoke = PendingRevoke(targets)) }
    }

    fun requestRevokeAllForDetail() {
        val app = _uiState.value.detailApp ?: return
        if (!_uiState.value.canMutate) {
            _uiState.update { it.copy(message = "capability_missing") }
            return
        }
        val targets = app.dangerousGranted.map { PermissionMutationTarget(app.packageName, it.name) }
        if (targets.isEmpty()) return
        _uiState.update { it.copy(pendingRevoke = PendingRevoke(targets)) }
    }

    fun dismissRevokeConfirm() = _uiState.update { it.copy(pendingRevoke = null) }

    fun confirmRevoke() {
        val pending = _uiState.value.pendingRevoke ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(running = true, pendingRevoke = null, message = null) }
            val results = repository.revoke(pending.targets)
            val ok = results.count { it.success }
            val fail = results.size - ok
            _uiState.update {
                it.copy(
                    running = false,
                    selectedPermissions = emptySet(),
                    message = "done:$ok:$fail",
                )
            }
            refresh()
        }
    }

    fun requestSetAppOp() {
        val state = _uiState.value
        val pkg = state.selectedPackage ?: return
        if (!state.canMutate) {
            _uiState.update { it.copy(message = "capability_missing") }
            return
        }
        _uiState.update {
            it.copy(
                pendingAppOp = PendingAppOp(pkg, state.selectedAppOp, state.selectedAppOpMode),
            )
        }
    }

    fun dismissAppOpConfirm() = _uiState.update { it.copy(pendingAppOp = null) }

    fun confirmSetAppOp() {
        val pending = _uiState.value.pendingAppOp ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(running = true, pendingAppOp = null, message = null) }
            val result = repository.setAppOp(pending.packageName, pending.op, pending.mode)
            val ok = if (result.success) 1 else 0
            val fail = 1 - ok
            _uiState.update {
                it.copy(running = false, message = "done:$ok:$fail")
            }
        }
    }

    fun clearMessage() = _uiState.update { it.copy(message = null) }
}

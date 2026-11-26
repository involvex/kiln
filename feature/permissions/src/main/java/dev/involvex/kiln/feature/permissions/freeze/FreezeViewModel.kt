package dev.involvex.kiln.feature.permissions.freeze

import android.os.Process
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.core.model.AndroidUserInfo
import dev.involvex.kiln.core.model.DualUserInstallGate
import dev.involvex.kiln.core.model.FreezeApp
import dev.involvex.kiln.core.model.FreezeGate
import dev.involvex.kiln.core.model.FreezeListFilter
import dev.involvex.kiln.core.model.FreezeListFilterLogic
import dev.involvex.kiln.core.model.PrivilegeCapability
import dev.involvex.kiln.core.model.PrivilegedExecutor
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class FreezeViewModel @Inject constructor(private val repository: FreezeRepository, private val privilegedExecutor: PrivilegedExecutor) :
    ViewModel() {
    private val _uiState = MutableStateFlow(
        FreezeUiState(canMutate = repository.canMutate()),
    )
    val uiState: StateFlow<FreezeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            privilegedExecutor.observeStatus().collect { status ->
                _uiState.update {
                    it.copy(canMutate = FreezeGate.canMutate(status.capabilities))
                }
            }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val includeSystem = _uiState.value.includeSystem
            _uiState.update { it.copy(isLoading = true, message = null) }
            val apps = repository.loadApps(includeSystem)
            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    apps = apps,
                    filteredApps = FreezeListFilterLogic.filter(apps, state.query, state.freezeFilter),
                    canMutate = repository.canMutate(),
                )
            }
        }
    }

    fun setQuery(query: String) {
        _uiState.update { state ->
            state.copy(
                query = query,
                filteredApps = FreezeListFilterLogic.filter(state.apps, query, state.freezeFilter),
            )
        }
    }

    fun setIncludeSystem(include: Boolean) {
        if (_uiState.value.includeSystem == include) return
        _uiState.update { it.copy(includeSystem = include) }
        refresh()
    }

    fun setFreezeFilter(filter: FreezeListFilter) {
        _uiState.update { state ->
            state.copy(
                freezeFilter = filter,
                filteredApps = FreezeListFilterLogic.filter(state.apps, state.query, filter),
            )
        }
    }

    fun requestFreeze(packageName: String) {
        if (!_uiState.value.canMutate) return
        viewModelScope.launch {
            beginPendingAction(
                packageName = packageName,
                freeze = true,
                users = loadCensusUsersForPicker(),
            )
        }
    }

    fun requestUnfreeze(packageName: String) {
        if (!_uiState.value.canMutate) return
        viewModelScope.launch {
            val users = loadCensusUsersForPicker()
            if (users.isEmpty()) {
                applyFrozen(packageName, frozen = false, targetUserId = currentUserId())
            } else {
                beginPendingAction(packageName = packageName, freeze = false, users = users)
            }
        }
    }

    fun selectPendingUser(userId: Int) {
        _uiState.update { state ->
            val pending = state.pendingFreezeAction ?: return@update state
            state.copy(
                pendingFreezeAction = pending.copy(
                    selectedUserId = DualUserInstallGate.resolveTargetUserId(
                        selectedUserId = userId,
                        users = pending.users,
                        currentUserId = currentUserId(),
                    ),
                ),
            )
        }
    }

    fun dismissFreezeConfirm() {
        _uiState.update { it.copy(pendingFreezeAction = null) }
    }

    fun confirmFreeze() {
        val pending = _uiState.value.pendingFreezeAction ?: return
        val userId = DualUserInstallGate.resolveTargetUserId(
            selectedUserId = pending.selectedUserId,
            users = pending.users,
            currentUserId = currentUserId(),
        )
        _uiState.update { it.copy(pendingFreezeAction = null) }
        applyFrozen(pending.packageName, frozen = pending.freeze, targetUserId = userId)
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null, messageIsError = false) }
    }

    private fun beginPendingAction(packageName: String, freeze: Boolean, users: List<AndroidUserInfo>) {
        _uiState.update {
            it.copy(
                pendingFreezeAction = PendingFreezeAction(
                    packageName = packageName,
                    freeze = freeze,
                    users = users,
                    selectedUserId = currentUserId(),
                ),
                message = null,
            )
        }
    }

    private suspend fun loadCensusUsersForPicker(): List<AndroidUserInfo> {
        val caps = privilegedExecutor.currentStatus().capabilities
        val hasDebloat = PrivilegeCapability.DEBLOAT in caps
        if (PrivilegeCapability.RUN_SHELL_DIAGNOSTIC !in caps) return emptyList()
        val users = when (val result = privilegedExecutor.listAndroidUsers()) {
            is Result.Success -> result.value
            is Result.Failure -> emptyList()
        }
        return if (DualUserInstallGate.showPicker(users, hasDebloat)) users else emptyList()
    }

    private fun currentUserId(): Int = Process.myUid() / ANDROID_PER_USER_RANGE

    private fun applyFrozen(packageName: String, frozen: Boolean, targetUserId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(mutatingPackage = packageName, message = null) }
            when (val result = repository.setFrozen(packageName, frozen, targetUserId)) {
                is Result.Failure -> _uiState.update {
                    it.copy(
                        mutatingPackage = null,
                        message = result.message,
                        messageIsError = true,
                    )
                }

                is Result.Success -> {
                    patchApp(packageName) { it.copy(isFrozen = frozen) }
                    _uiState.update { it.copy(mutatingPackage = null) }
                }
            }
        }
    }

    private fun patchApp(packageName: String, transform: (FreezeApp) -> FreezeApp) {
        _uiState.update { state ->
            val apps = state.apps.map { if (it.packageName == packageName) transform(it) else it }
            state.copy(
                apps = apps,
                filteredApps = FreezeListFilterLogic.filter(apps, state.query, state.freezeFilter),
            )
        }
    }

    private companion object {
        const val ANDROID_PER_USER_RANGE = 100_000
    }
}

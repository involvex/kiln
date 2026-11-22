package dev.involvex.kiln.feature.permissions.processes

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.core.model.ActivityLogEvents
import dev.involvex.kiln.core.model.ActivityLogRecorder
import dev.involvex.kiln.core.model.ActivityLogType
import dev.involvex.kiln.core.model.ForceStopConfirmGate
import dev.involvex.kiln.core.model.PrivilegedExecutor
import dev.involvex.kiln.core.model.ProcessStatsGate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ProcessesViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val privilegedExecutor: PrivilegedExecutor,
    private val activityLog: ActivityLogRecorder,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProcessesUiState())
    val uiState: StateFlow<ProcessesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            privilegedExecutor.observeStatus().collect { status ->
                _uiState.update {
                    it.copy(
                        canList = ProcessStatsGate.canList(status.capabilities),
                        canForceStop = ProcessStatsGate.canForceStop(status.capabilities),
                    )
                }
            }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val caps = privilegedExecutor.currentStatus().capabilities
            if (!ProcessStatsGate.canList(caps)) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        canList = false,
                        canForceStop = ProcessStatsGate.canForceStop(caps),
                        processes = emptyList(),
                        errorKey = null,
                    )
                }
                return@launch
            }
            _uiState.update { it.copy(isLoading = true, message = null, errorKey = null) }
            when (val result = privilegedExecutor.listProcessStats(LIMIT)) {
                is Result.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            processes = emptyList(),
                            errorKey = result.message.substringBefore(':').ifBlank { "shell_error" },
                            canList = true,
                            canForceStop = ProcessStatsGate.canForceStop(caps),
                        )
                    }
                }

                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            processes = result.value,
                            canList = true,
                            canForceStop = ProcessStatsGate.canForceStop(caps),
                        )
                    }
                }
            }
        }
    }

    fun requestForceStop(packageName: String) {
        if (!_uiState.value.canForceStop) return
        if (packageName == context.packageName) return
        _uiState.update { it.copy(pendingKillPackage = packageName, message = null) }
    }

    fun dismissForceStop() {
        _uiState.update { it.copy(pendingKillPackage = null) }
    }

    fun confirmForceStop() {
        val pkg = _uiState.value.pendingKillPackage ?: return
        if (!ForceStopConfirmGate.mayExecute(
                pendingPackage = pkg,
                targetPackage = pkg,
                kilnPackageName = context.packageName,
            )
        ) {
            _uiState.update { it.copy(pendingKillPackage = null, message = "force_stop_blocked") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(pendingKillPackage = null, isLoading = true) }
            when (val result = privilegedExecutor.forceStopPackage(pkg)) {
                is Result.Failure -> {
                    activityLog.record(
                        ActivityLogEvents.create(
                            type = ActivityLogType.SHELL,
                            action = "force_stop",
                            success = false,
                            packageName = pkg,
                            detailKey = result.message,
                        ),
                    )
                    _uiState.update {
                        it.copy(isLoading = false, message = result.message)
                    }
                }

                is Result.Success -> {
                    activityLog.record(
                        ActivityLogEvents.create(
                            type = ActivityLogType.SHELL,
                            action = "force_stop",
                            success = true,
                            packageName = pkg,
                        ),
                    )
                    refresh()
                    _uiState.update { it.copy(message = "force_stop_ok", isLoading = false) }
                }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    companion object {
        const val LIMIT = 24
    }
}

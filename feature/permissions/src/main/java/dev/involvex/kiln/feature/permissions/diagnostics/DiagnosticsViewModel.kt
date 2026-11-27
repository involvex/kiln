package dev.involvex.kiln.feature.permissions.diagnostics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.core.model.DiagnosticsGate
import dev.involvex.kiln.core.model.PrivilegedExecutor
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(private val privilegedExecutor: PrivilegedExecutor) : ViewModel() {
    private val _uiState = MutableStateFlow(DiagnosticsUiState())
    val uiState: StateFlow<DiagnosticsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            privilegedExecutor.observeStatus().collect { status ->
                _uiState.update {
                    it.copy(canRun = DiagnosticsGate.canRun(status.capabilities))
                }
            }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val caps = privilegedExecutor.currentStatus().capabilities
            if (!DiagnosticsGate.canRun(caps)) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        canRun = false,
                        battery = null,
                        netstats = null,
                        top = null,
                        batteryError = null,
                        netstatsError = null,
                        topError = null,
                    )
                }
                return@launch
            }
            _uiState.update {
                it.copy(
                    isLoading = true,
                    canRun = true,
                    batteryError = null,
                    netstatsError = null,
                    topError = null,
                )
            }
            coroutineScope {
                val batteryDeferred = async { privilegedExecutor.readBatteryDump() }
                val netDeferred = async { privilegedExecutor.readNetstatsDump() }
                val topDeferred = async { privilegedExecutor.readTopDump() }
                val battery = batteryDeferred.await()
                val net = netDeferred.await()
                val top = topDeferred.await()
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        battery = (battery as? Result.Success)?.value,
                        batteryError = (battery as? Result.Failure)?.message?.shortError(),
                        netstats = (net as? Result.Success)?.value,
                        netstatsError = (net as? Result.Failure)?.message?.shortError(),
                        top = (top as? Result.Success)?.value,
                        topError = (top as? Result.Failure)?.message?.shortError(),
                    )
                }
            }
        }
    }

    private fun String.shortError(): String = substringBefore(':').ifBlank { "shell_error" }
}

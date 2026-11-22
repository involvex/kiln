package dev.involvex.kiln.feature.permissions.users

import android.os.Process
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.core.model.PrivilegedExecutor
import dev.involvex.kiln.core.model.ProcessStatsGate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class UsersViewModel @Inject constructor(private val privilegedExecutor: PrivilegedExecutor) : ViewModel() {
    private val _uiState = MutableStateFlow(UsersUiState(currentUserId = currentUserId()))
    val uiState: StateFlow<UsersUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            privilegedExecutor.observeStatus().collect { status ->
                _uiState.update {
                    it.copy(canList = ProcessStatsGate.canList(status.capabilities))
                }
            }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val caps = privilegedExecutor.currentStatus().capabilities
            val currentId = currentUserId()
            if (!ProcessStatsGate.canList(caps)) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        canList = false,
                        currentUserId = currentId,
                        users = emptyList(),
                        errorKey = null,
                    )
                }
                return@launch
            }
            _uiState.update { it.copy(isLoading = true, errorKey = null, currentUserId = currentId) }
            when (val result = privilegedExecutor.listAndroidUsers()) {
                is Result.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            canList = true,
                            users = emptyList(),
                            errorKey = result.message.substringBefore(':').ifBlank { "shell_error" },
                        )
                    }
                }

                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            canList = true,
                            users = result.value,
                            errorKey = null,
                        )
                    }
                }
            }
        }
    }

    companion object {
        /** Android multi-user: userId = uid / 100000 (API 17+). */
        fun currentUserId(): Int = Process.myUid() / 100_000
    }
}

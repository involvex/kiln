package dev.involvex.kiln.feature.storage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.core.model.ActivityLogEvents
import dev.involvex.kiln.core.model.ActivityLogRecorder
import dev.involvex.kiln.core.model.ActivityLogType
import dev.involvex.kiln.core.model.AppStorageStats
import dev.involvex.kiln.core.model.PrivilegeCapability
import dev.involvex.kiln.core.model.PrivilegedExecutor
import dev.involvex.kiln.feature.storage.data.StorageStatsRepository
import dev.involvex.kiln.feature.storage.data.UsageAccessChecker
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class StorageViewModel @Inject constructor(
    private val repository: StorageStatsRepository,
    private val usageAccessChecker: UsageAccessChecker,
    private val privilegedExecutor: PrivilegedExecutor,
    private val activityLog: ActivityLogRecorder,
) : ViewModel() {
    private val _uiState = MutableStateFlow(StorageUiState())
    val uiState: StateFlow<StorageUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            privilegedExecutor.observeStatus().collect { status ->
                _uiState.update {
                    it.copy(canClearPrivileged = PrivilegeCapability.CLEAR_CACHE in status.capabilities)
                }
            }
        }
        refreshPermissionAndLoad()
    }

    fun refreshPermissionAndLoad() {
        val granted = usageAccessChecker.hasUsageAccess()
        _uiState.update {
            it.copy(
                permission = if (granted) {
                    StoragePermissionState.GRANTED
                } else {
                    StoragePermissionState.MISSING
                },
                error = null,
            )
        }
        if (granted) {
            load()
        }
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, message = null) }
            when (val result = repository.loadOverview(_uiState.value.includeSystem)) {
                is Result.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.message,
                            apps = emptyList(),
                        )
                    }
                }

                is Result.Success -> {
                    val overview = result.value
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            apps = overview.apps,
                            totalAppBytes = overview.totalAppBytes,
                            totalDataBytes = overview.totalDataBytes,
                            totalCacheBytes = overview.totalCacheBytes,
                        )
                    }
                }
            }
        }
    }

    fun setIncludeSystem(include: Boolean) {
        _uiState.update { it.copy(includeSystem = include) }
        if (_uiState.value.permission == StoragePermissionState.GRANTED) {
            load()
        }
    }

    fun setQuery(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun usageAccessIntent() = usageAccessChecker.usageAccessSettingsIntent()

    fun appDetailsIntent(packageName: String) = usageAccessChecker.appDetailsIntent(packageName)

    fun requestClearApp(app: AppStorageStats) {
        _uiState.update {
            it.copy(
                pendingClear = PendingClearAction(
                    packageName = app.packageName,
                    label = app.label,
                    cacheBytes = app.cacheBytes,
                ),
            )
        }
    }

    fun requestClearAllCaches() {
        val totalCache = _uiState.value.totalCacheBytes
        _uiState.update {
            it.copy(
                pendingClear = PendingClearAction(
                    packageName = null,
                    label = "",
                    cacheBytes = totalCache,
                ),
            )
        }
    }

    fun dismissClear() {
        _uiState.update { it.copy(pendingClear = null) }
    }

    fun confirmClear() {
        val pending = _uiState.value.pendingClear ?: return
        if (!_uiState.value.canClearPrivileged) {
            dismissClear()
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(clearing = true, message = null, error = null) }
            when (val result = privilegedExecutor.clearCache(pending.packageName)) {
                is Result.Failure -> {
                    activityLog.record(
                        ActivityLogEvents.create(
                            type = ActivityLogType.CACHE,
                            action = "clear_cache",
                            success = false,
                            packageName = pending.packageName,
                            detailKey = result.message,
                        ),
                    )
                    _uiState.update {
                        it.copy(
                            clearing = false,
                            pendingClear = null,
                            message = result.message,
                        )
                    }
                }

                is Result.Success -> {
                    activityLog.record(
                        ActivityLogEvents.create(
                            type = ActivityLogType.CACHE,
                            action = "clear_cache",
                            success = true,
                            packageName = pending.packageName,
                            detailKey = "bytes:${result.value}",
                        ),
                    )
                    _uiState.update {
                        it.copy(
                            clearing = false,
                            pendingClear = null,
                            message = null,
                        )
                    }
                    load()
                }
            }
        }
    }
}

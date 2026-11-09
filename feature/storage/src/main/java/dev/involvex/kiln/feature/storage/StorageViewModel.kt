package dev.involvex.kiln.feature.storage

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.core.model.ActivityLogEvents
import dev.involvex.kiln.core.model.ActivityLogRecorder
import dev.involvex.kiln.core.model.ActivityLogType
import dev.involvex.kiln.core.model.AppStorageStats
import dev.involvex.kiln.core.model.OrphanStorageDir
import dev.involvex.kiln.core.model.PrivilegeCapability
import dev.involvex.kiln.core.model.PrivilegedExecutor
import dev.involvex.kiln.core.model.StorageOrphanGate
import dev.involvex.kiln.feature.storage.data.ApkLeftoverRepository
import dev.involvex.kiln.feature.storage.data.OrphanStorageRepository
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
    private val leftoverRepository: ApkLeftoverRepository,
    private val orphanRepository: OrphanStorageRepository,
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
                    it.copy(
                        canClearPrivileged = PrivilegeCapability.CLEAR_CACHE in status.capabilities,
                        canAccessAndroidData = StorageOrphanGate.canAccessAndroidData(status.capabilities),
                    )
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
        if (_uiState.value.canAccessAndroidData) {
            loadOrphans()
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

    fun scanLeftovers(treeUri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(leftoversLoading = true, message = null) }
            when (val result = leftoverRepository.scanTree(treeUri)) {
                is Result.Failure -> {
                    _uiState.update {
                        it.copy(leftoversLoading = false, message = result.message, leftovers = emptyList())
                    }
                }

                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            leftoversLoading = false,
                            leftovers = result.value,
                            selectedLeftoverIds = emptySet(),
                        )
                    }
                }
            }
        }
    }

    fun toggleLeftover(id: String) {
        _uiState.update { state ->
            val next = state.selectedLeftoverIds.toMutableSet()
            if (!next.add(id)) next.remove(id)
            state.copy(selectedLeftoverIds = next)
        }
    }

    fun requestDeleteSelectedLeftovers() {
        val state = _uiState.value
        val items = state.leftovers.filter { it.id in state.selectedLeftoverIds }
        if (items.isEmpty()) return
        _uiState.update { it.copy(pendingDelete = PendingStorageDelete.Leftovers(items)) }
    }

    fun loadOrphans() {
        if (!_uiState.value.canAccessAndroidData) {
            _uiState.update {
                it.copy(orphans = emptyList(), orphansError = "capability_missing", orphansLoading = false)
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(orphansLoading = true, orphansError = null) }
            when (val result = orphanRepository.loadOrphans()) {
                is Result.Failure -> {
                    _uiState.update {
                        it.copy(
                            orphansLoading = false,
                            orphans = emptyList(),
                            orphansError = result.message,
                        )
                    }
                }

                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            orphansLoading = false,
                            orphans = result.value,
                            selectedOrphans = emptySet(),
                            orphansError = null,
                        )
                    }
                }
            }
        }
    }

    fun toggleOrphan(orphan: OrphanStorageDir) {
        val key = _uiState.value.orphanKey(orphan)
        _uiState.update { state ->
            val next = state.selectedOrphans.toMutableSet()
            if (!next.add(key)) next.remove(key)
            state.copy(selectedOrphans = next)
        }
    }

    fun requestDeleteSelectedOrphans() {
        val state = _uiState.value
        if (!state.canAccessAndroidData) {
            _uiState.update { it.copy(message = "capability_missing") }
            return
        }
        val items = state.orphans.filter { state.orphanKey(it) in state.selectedOrphans }
        if (items.isEmpty()) return
        _uiState.update { it.copy(pendingDelete = PendingStorageDelete.Orphans(items)) }
    }

    fun dismissDelete() {
        _uiState.update { it.copy(pendingDelete = null) }
    }

    fun confirmDelete() {
        val pending = _uiState.value.pendingDelete ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(deleting = true, pendingDelete = null, message = null) }
            when (pending) {
                is PendingStorageDelete.Leftovers -> {
                    val results = leftoverRepository.delete(pending.items)
                    val ok = results.count { it.second }
                    val fail = results.size - ok
                    _uiState.update {
                        it.copy(
                            deleting = false,
                            selectedLeftoverIds = emptySet(),
                            message = "done:$ok:$fail",
                        )
                    }
                    // Refresh leftover list without requiring a new tree pick if empties remain.
                    val remaining = results.filterNot { it.second }.map { it.first }
                    _uiState.update { it.copy(leftovers = remaining) }
                }

                is PendingStorageDelete.Orphans -> {
                    val results = orphanRepository.delete(pending.items)
                    val ok = results.count { it.second }
                    val fail = results.size - ok
                    _uiState.update {
                        it.copy(
                            deleting = false,
                            selectedOrphans = emptySet(),
                            message = "done:$ok:$fail",
                        )
                    }
                    loadOrphans()
                }
            }
        }
    }

    fun clearMessage() = _uiState.update { it.copy(message = null) }
}

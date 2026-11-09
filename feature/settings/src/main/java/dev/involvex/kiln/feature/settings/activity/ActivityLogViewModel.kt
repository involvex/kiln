package dev.involvex.kiln.feature.settings.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.involvex.kiln.core.model.ActivityLogQuery
import dev.involvex.kiln.core.model.ActivityLogRepository
import dev.involvex.kiln.core.model.ActivityLogType
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ActivityLogViewModel @Inject constructor(private val repository: ActivityLogRepository) : ViewModel() {
    private val query = MutableStateFlow("")
    private val typeFilter = MutableStateFlow<ActivityLogType?>(null)
    private val confirmClear = MutableStateFlow(false)
    private val clearing = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val events = combine(query, typeFilter) { q, type ->
        ActivityLogQuery(search = q, type = type)
    }.flatMapLatest { repository.observe(it) }

    val uiState: StateFlow<ActivityLogUiState> = combine(
        events,
        query,
        typeFilter,
        confirmClear,
        clearing,
    ) { list, q, type, confirm, isClearing ->
        ActivityLogUiState(
            events = list,
            query = q,
            typeFilter = type,
            confirmClear = confirm,
            clearing = isClearing,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ActivityLogUiState(),
    )

    fun setQuery(value: String) {
        query.value = value
    }

    fun setTypeFilter(type: ActivityLogType?) {
        typeFilter.value = type
    }

    fun requestClear() {
        confirmClear.value = true
    }

    fun dismissClear() {
        confirmClear.value = false
    }

    fun confirmClear() {
        viewModelScope.launch {
            clearing.value = true
            repository.clearAll()
            clearing.value = false
            confirmClear.value = false
        }
    }
}

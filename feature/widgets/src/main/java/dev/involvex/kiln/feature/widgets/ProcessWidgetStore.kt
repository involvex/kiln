package dev.involvex.kiln.feature.widgets

import dev.involvex.kiln.core.model.ProcessStat
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ProcessWidgetSnapshot(
    val rows: List<ProcessStat> = emptyList(),
    /** Stable key for UI copy when the last poll failed; never raw shell output. */
    val lastErrorKey: String? = null,
)

/** In-memory last process snapshot for Glance (not persisted). */
@Singleton
class ProcessWidgetStore @Inject constructor() {
    private val _snapshot = MutableStateFlow(ProcessWidgetSnapshot())
    val snapshot: StateFlow<ProcessWidgetSnapshot> = _snapshot.asStateFlow()

    fun current(): ProcessWidgetSnapshot = _snapshot.value

    fun publish(rows: List<ProcessStat>) {
        _snapshot.value = ProcessWidgetSnapshot(rows = rows, lastErrorKey = null)
    }

    fun publishFailure(errorKey: String) {
        _snapshot.value = ProcessWidgetSnapshot(
            rows = _snapshot.value.rows,
            lastErrorKey = errorKey,
        )
    }

    fun clear() {
        _snapshot.value = ProcessWidgetSnapshot()
    }
}

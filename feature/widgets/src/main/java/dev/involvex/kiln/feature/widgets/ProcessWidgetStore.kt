package dev.involvex.kiln.feature.widgets

import dev.involvex.kiln.core.model.ProcessStat
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** In-memory last process snapshot for Glance (not persisted). */
@Singleton
class ProcessWidgetStore @Inject constructor() {
    private val _rows = MutableStateFlow<List<ProcessStat>>(emptyList())
    val rows: StateFlow<List<ProcessStat>> = _rows.asStateFlow()

    fun current(): List<ProcessStat> = _rows.value

    fun publish(rows: List<ProcessStat>) {
        _rows.value = rows
    }
}

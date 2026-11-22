package dev.involvex.kiln.feature.permissions.processes

import dev.involvex.kiln.core.model.ProcessStat

data class ProcessesUiState(
    val isLoading: Boolean = false,
    val canList: Boolean = false,
    val canForceStop: Boolean = false,
    val processes: List<ProcessStat> = emptyList(),
    val pendingKillPackage: String? = null,
    val message: String? = null,
    val errorKey: String? = null,
)

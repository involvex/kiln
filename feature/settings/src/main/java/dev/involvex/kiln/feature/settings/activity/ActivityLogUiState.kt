package dev.involvex.kiln.feature.settings.activity

import dev.involvex.kiln.core.model.ActivityLogEvent
import dev.involvex.kiln.core.model.ActivityLogType

data class ActivityLogUiState(
    val events: List<ActivityLogEvent> = emptyList(),
    val query: String = "",
    val typeFilter: ActivityLogType? = null,
    val confirmClear: Boolean = false,
    val clearing: Boolean = false,
)

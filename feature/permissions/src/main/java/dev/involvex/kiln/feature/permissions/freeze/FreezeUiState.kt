package dev.involvex.kiln.feature.permissions.freeze

import dev.involvex.kiln.core.model.FreezeApp
import dev.involvex.kiln.core.model.FreezeListFilter

data class FreezeUiState(
    val isLoading: Boolean = true,
    val canMutate: Boolean = false,
    val includeSystem: Boolean = false,
    val freezeFilter: FreezeListFilter = FreezeListFilter.ALL,
    val query: String = "",
    val apps: List<FreezeApp> = emptyList(),
    val filteredApps: List<FreezeApp> = emptyList(),
    val pendingFreezePackage: String? = null,
    val mutatingPackage: String? = null,
    val message: String? = null,
    val messageIsError: Boolean = false,
)

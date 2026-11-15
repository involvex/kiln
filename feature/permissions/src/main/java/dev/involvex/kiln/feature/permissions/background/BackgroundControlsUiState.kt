package dev.involvex.kiln.feature.permissions.background

import dev.involvex.kiln.core.model.BackgroundControlsApp

data class BackgroundControlsUiState(
    val isLoading: Boolean = true,
    val canMutate: Boolean = false,
    val isHyperOs: Boolean = false,
    val includeSystem: Boolean = false,
    val query: String = "",
    val apps: List<BackgroundControlsApp> = emptyList(),
    val filteredApps: List<BackgroundControlsApp> = emptyList(),
    val mutatingPackage: String? = null,
    val message: String? = null,
    val messageIsError: Boolean = false,
)

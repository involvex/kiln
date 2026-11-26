package dev.involvex.kiln.feature.permissions.freeze

import dev.involvex.kiln.core.model.AndroidUserInfo
import dev.involvex.kiln.core.model.FreezeApp
import dev.involvex.kiln.core.model.FreezeListFilter

data class PendingFreezeAction(
    val packageName: String,
    val freeze: Boolean,
    /** Empty when single-user / no census — no picker. */
    val users: List<AndroidUserInfo> = emptyList(),
    val selectedUserId: Int = 0,
)

data class FreezeUiState(
    val isLoading: Boolean = true,
    val canMutate: Boolean = false,
    val includeSystem: Boolean = false,
    val freezeFilter: FreezeListFilter = FreezeListFilter.ALL,
    val query: String = "",
    val apps: List<FreezeApp> = emptyList(),
    val filteredApps: List<FreezeApp> = emptyList(),
    val pendingFreezeAction: PendingFreezeAction? = null,
    val mutatingPackage: String? = null,
    val message: String? = null,
    val messageIsError: Boolean = false,
)

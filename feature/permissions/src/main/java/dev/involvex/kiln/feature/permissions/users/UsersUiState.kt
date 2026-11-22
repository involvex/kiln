package dev.involvex.kiln.feature.permissions.users

import dev.involvex.kiln.core.model.AndroidUserInfo

data class UsersUiState(
    val isLoading: Boolean = false,
    val canList: Boolean = false,
    val currentUserId: Int = 0,
    val users: List<AndroidUserInfo> = emptyList(),
    val errorKey: String? = null,
)

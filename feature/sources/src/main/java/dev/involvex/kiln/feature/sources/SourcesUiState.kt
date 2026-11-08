package dev.involvex.kiln.feature.sources

data class RepoUiModel(
    val id: String,
    val name: String,
    val address: String,
    val fingerprint: String,
    val enabled: Boolean,
    val lastSyncedLabel: String?,
    val priority: Int,
)

data class SourcesUiState(
    val repos: List<RepoUiModel> = emptyList(),
    val isSyncing: Boolean = false,
    val message: String? = null,
    val githubTokenSet: Boolean = false,
    val addName: String = "",
    val addAddress: String = "",
    val addFingerprint: String = "",
    val obtainiumText: String = "",
    val githubTokenInput: String = "",
)

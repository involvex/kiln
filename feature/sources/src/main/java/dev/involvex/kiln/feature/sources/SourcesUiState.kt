package dev.involvex.kiln.feature.sources

import dev.involvex.kiln.core.model.WatchlistEntry

data class RepoUiModel(
    val id: String,
    val name: String,
    val address: String,
    val fingerprint: String,
    val enabled: Boolean,
    val lastSyncedLabel: String?,
    val priority: Int,
    val mirrorsCsv: String = "",
    val mirrorCount: Int = 0,
)

data class SourcesUiState(
    val repos: List<RepoUiModel> = emptyList(),
    val watchlist: List<WatchlistEntry> = emptyList(),
    val isSyncing: Boolean = false,
    val message: String? = null,
    val githubTokenSet: Boolean = false,
    val addName: String = "",
    val addAddress: String = "",
    val addFingerprint: String = "",
    val obtainiumText: String = "",
    val githubTokenInput: String = "",
)

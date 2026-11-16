package dev.involvex.kiln.feature.sources

import dev.involvex.kiln.core.model.WatchlistEntry
import dev.involvex.kiln.feature.sources.github.StarredApkHint

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

data class StarredRepoUiModel(
    val ownerRepo: String,
    val description: String?,
    val fork: Boolean,
    val alreadyManaged: Boolean,
    val selected: Boolean,
    val apkHint: StarredApkHint = StarredApkHint.UNKNOWN,
)

data class SourcesUiState(
    val repos: List<RepoUiModel> = emptyList(),
    val watchlist: List<WatchlistEntry> = emptyList(),
    val isSyncing: Boolean = false,
    val syncProgressCurrent: Int = 0,
    val syncProgressTotal: Int = 0,
    val syncProgressLabel: String? = null,
    val message: String? = null,
    val githubTokenSet: Boolean = false,
    val addName: String = "",
    val addAddress: String = "",
    val addFingerprint: String = "",
    val obtainiumText: String = "",
    val githubTokenInput: String = "",
    val starredRepos: List<StarredRepoUiModel> = emptyList(),
    val starredLoading: Boolean = false,
    val starredScanningApk: Boolean = false,
    val starredLoaded: Boolean = false,
)

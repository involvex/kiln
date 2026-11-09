package dev.involvex.kiln.feature.sources.discovery

enum class DiscoverySourceTab {
    FDROID,
    GITHUB,
}

data class DiscoveryGithubRow(
    val ownerRepo: String,
    val packageName: String,
    val displayName: String,
    val tagName: String?,
    val assetName: String?,
    val sizeBytes: Long?,
    val downloadUrl: String?,
    val saved: Boolean,
    val releaseNotes: String? = null,
)

data class DiscoveryUiState(
    val sourceTab: DiscoverySourceTab = DiscoverySourceTab.FDROID,
    val query: String = "",
    val selectedCategory: String = "",
    val categories: List<String> = emptyList(),
    val results: List<DiscoveryAppRow> = emptyList(),
    val packageCount: Int = 0,
    val isRefreshing: Boolean = false,
    val isSearching: Boolean = false,
    val githubQuery: String = "",
    val githubResults: List<DiscoveryGithubRow> = emptyList(),
    val savedGithub: List<DiscoveryGithubRow> = emptyList(),
    val isLookingUpGithub: Boolean = false,
    val message: String? = null,
    val messageIsError: Boolean = false,
)

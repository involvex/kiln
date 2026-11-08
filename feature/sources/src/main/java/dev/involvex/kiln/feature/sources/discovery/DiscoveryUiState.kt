package dev.involvex.kiln.feature.sources.discovery

data class DiscoveryUiState(
    val query: String = "",
    val selectedCategory: String = "",
    val categories: List<String> = emptyList(),
    val results: List<DiscoveryAppRow> = emptyList(),
    val packageCount: Int = 0,
    val isRefreshing: Boolean = false,
    val isSearching: Boolean = false,
    val message: String? = null,
    val messageIsError: Boolean = false,
)

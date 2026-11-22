package dev.involvex.kiln.feature.sources

/**
 * Client-side filter for starred GitHub rows. Empty/blank query keeps every row.
 * Matches [StarredRepoUiModel.ownerRepo] and [StarredRepoUiModel.description] case-insensitively.
 */
fun filterStarredRepos(repos: List<StarredRepoUiModel>, query: String): List<StarredRepoUiModel> {
    val normalized = query.trim()
    if (normalized.isEmpty()) return repos
    return repos.filter { row ->
        row.ownerRepo.contains(normalized, ignoreCase = true) ||
            row.description.orEmpty().contains(normalized, ignoreCase = true)
    }
}

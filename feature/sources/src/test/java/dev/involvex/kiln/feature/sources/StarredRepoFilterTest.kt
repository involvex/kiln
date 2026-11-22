package dev.involvex.kiln.feature.sources

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StarredRepoFilterTest {

    private val repos = listOf(
        starred("jqssun/android-titanium-browser", "Chromium-based browser"),
        starred("involvex/kiln", "Android app manager"),
        starred("fdroid/fdroidclient", null),
    )

    @Test
    fun `empty query returns all`() {
        assertEquals(repos, filterStarredRepos(repos, ""))
        assertEquals(repos, filterStarredRepos(repos, "   "))
    }

    @Test
    fun `matches ownerRepo case-insensitively`() {
        assertEquals(
            listOf(repos[0]),
            filterStarredRepos(repos, "TITANIUM"),
        )
    }

    @Test
    fun `matches description case-insensitively`() {
        assertEquals(
            listOf(repos[1]),
            filterStarredRepos(repos, "app manager"),
        )
    }

    @Test
    fun `no match returns empty`() {
        assertEquals(emptyList<StarredRepoUiModel>(), filterStarredRepos(repos, "zzzz-missing"))
    }

    private fun starred(ownerRepo: String, description: String?) = StarredRepoUiModel(
        ownerRepo = ownerRepo,
        description = description,
        fork = false,
        alreadyManaged = false,
        selected = false,
    )
}

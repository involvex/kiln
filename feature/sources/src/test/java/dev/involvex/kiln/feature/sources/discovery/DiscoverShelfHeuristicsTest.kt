package dev.involvex.kiln.feature.sources.discovery

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DiscoverShelfHeuristicsTest {
    @Test
    fun matchesAi_detectsCommonTokens() {
        assertTrue(DiscoverShelfHeuristics.matchesAi("Local LLM Chat", null))
        assertTrue(DiscoverShelfHeuristics.matchesAi("Helper", "Uses OpenAI APIs"))
        assertTrue(DiscoverShelfHeuristics.matchesAi("Neural Network Lab", "demo"))
    }

    @Test
    fun matchesAi_ignoresUnrelated() {
        assertFalse(DiscoverShelfHeuristics.matchesAi("Mail Client", "IMAP and SMTP"))
        assertFalse(DiscoverShelfHeuristics.matchesAi("Paint", "drawing app"))
    }

    @Test
    fun showHomeShelves_requiresBlankQueryAndCategory() {
        assertTrue(
            DiscoveryUiState().showHomeShelves,
        )
        assertFalse(
            DiscoveryUiState(query = "fdroid").showHomeShelves,
        )
        assertFalse(
            DiscoveryUiState(selectedCategory = "Internet").showHomeShelves,
        )
        assertFalse(
            DiscoveryUiState(sourceTab = DiscoverySourceTab.GITHUB).showHomeShelves,
        )
    }
}

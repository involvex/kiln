package dev.involvex.kiln.feature.onboarding.match

import dev.involvex.kiln.core.model.MatchConfidence
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AutoMatchListFilterTest {
    private val high = candidate("com.high", "High App", MatchConfidence.HIGH)
    private val medium = candidate("com.medium", "Medium App", MatchConfidence.MEDIUM)
    private val warning = candidate("com.warn", "Warn App", MatchConfidence.WARNING)
    private val none = candidate("com.none", "None App", MatchConfidence.NONE)
    private val linkedHigh = high.copy(alreadyLinked = true)
    private val all = listOf(high, medium, warning, none, linkedHigh)

    @Test
    fun `hides linked by default`() {
        val result = AutoMatchListFilter.filter(
            candidates = all,
            searchQuery = "",
            confidenceFilter = AutoMatchConfidenceFilter.ALL,
            showLinked = false,
        )
        assertEquals(listOf(high, medium, warning, none), result)
        assertFalse(result.any { it.alreadyLinked })
    }

    @Test
    fun `show linked includes already linked rows`() {
        val result = AutoMatchListFilter.filter(
            candidates = all,
            searchQuery = "",
            confidenceFilter = AutoMatchConfidenceFilter.ALL,
            showLinked = true,
        )
        assertEquals(5, result.size)
        assertTrue(result.any { it.alreadyLinked })
    }

    @Test
    fun `search matches label case-insensitively`() {
        val result = AutoMatchListFilter.filter(
            candidates = all,
            searchQuery = "warn",
            confidenceFilter = AutoMatchConfidenceFilter.ALL,
            showLinked = false,
        )
        assertEquals(listOf(warning), result)
    }

    @Test
    fun `search matches package name`() {
        val result = AutoMatchListFilter.filter(
            candidates = all,
            searchQuery = "com.medium",
            confidenceFilter = AutoMatchConfidenceFilter.ALL,
            showLinked = false,
        )
        assertEquals(listOf(medium), result)
    }

    @Test
    fun `confidence filter restricts groups`() {
        val result = AutoMatchListFilter.filter(
            candidates = all,
            searchQuery = "",
            confidenceFilter = AutoMatchConfidenceFilter.WARNING,
            showLinked = false,
        )
        assertEquals(listOf(warning), result)
    }

    @Test
    fun `confidence and search combine`() {
        val result = AutoMatchListFilter.filter(
            candidates = all,
            searchQuery = "app",
            confidenceFilter = AutoMatchConfidenceFilter.HIGH,
            showLinked = false,
        )
        assertEquals(listOf(high), result)
    }

    @Test
    fun `empty query keeps confidence filter`() {
        assertTrue(
            AutoMatchListFilter.matchesConfidence(MatchConfidence.MEDIUM, AutoMatchConfidenceFilter.MEDIUM),
        )
        assertFalse(
            AutoMatchListFilter.matchesConfidence(MatchConfidence.HIGH, AutoMatchConfidenceFilter.NONE),
        )
    }

    private fun candidate(packageName: String, label: String, confidence: MatchConfidence): MatchCandidate = MatchCandidate(
        packageName = packageName,
        label = label,
        repoId = "fdroid",
        sourceLabel = "F-Droid",
        confidence = confidence,
    )
}

package dev.involvex.kiln.feature.onboarding.match

import dev.involvex.kiln.core.model.MatchConfidence

enum class AutoMatchConfidenceFilter {
    ALL,
    HIGH,
    MEDIUM,
    WARNING,
    NONE,
}

/**
 * Pure list filtering for Match installed apps (search, confidence chip, hide linked).
 */
object AutoMatchListFilter {
    fun filter(
        candidates: List<MatchCandidate>,
        searchQuery: String,
        confidenceFilter: AutoMatchConfidenceFilter,
        showLinked: Boolean,
    ): List<MatchCandidate> {
        val query = searchQuery.trim()
        return candidates.filter { candidate ->
            if (!showLinked && candidate.alreadyLinked) return@filter false
            if (!matchesConfidence(candidate.confidence, confidenceFilter)) return@filter false
            if (query.isEmpty()) return@filter true
            candidate.label.contains(query, ignoreCase = true) ||
                candidate.packageName.contains(query, ignoreCase = true)
        }
    }

    fun matchesConfidence(confidence: MatchConfidence, filter: AutoMatchConfidenceFilter): Boolean = when (filter) {
        AutoMatchConfidenceFilter.ALL -> true
        AutoMatchConfidenceFilter.HIGH -> confidence == MatchConfidence.HIGH
        AutoMatchConfidenceFilter.MEDIUM -> confidence == MatchConfidence.MEDIUM
        AutoMatchConfidenceFilter.WARNING -> confidence == MatchConfidence.WARNING
        AutoMatchConfidenceFilter.NONE -> confidence == MatchConfidence.NONE
    }
}

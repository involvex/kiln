package dev.involvex.kiln.feature.sources.discovery

/**
 * Pure shelf matching rules for Discover home. Not popularity / download charts.
 */
object DiscoverShelfHeuristics {
    private val AI_PATTERN = Regex(
        """(?i)(\bai\b|\bllm\b|\bgpt\b|machine\s+learning|neural\s+network|openai)""",
    )

    fun matchesAi(name: String, summary: String?): Boolean {
        val haystack = listOfNotNull(name, summary).joinToString(" ")
        return AI_PATTERN.containsMatchIn(haystack)
    }
}

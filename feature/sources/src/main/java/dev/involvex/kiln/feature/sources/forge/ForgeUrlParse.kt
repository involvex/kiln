package dev.involvex.kiln.feature.sources.forge

import dev.involvex.kiln.core.model.ReleaseForge

/**
 * Parse `owner/repo` or forge HTTPS/SSH URLs for GitHub, GitLab.com, and Codeberg.
 * Bare `owner/repo` (no host) defaults to [ReleaseForge.GITHUB] for backwards compatibility.
 */
object ForgeUrlParse {
    private val SLASH_FORM = Regex("""^([A-Za-z0-9_.-]+)/([A-Za-z0-9_.-]+)$""")
    private val GITHUB = Regex("""github\.com[/:]([^/]+)/([^/.\s?#]+)""", RegexOption.IGNORE_CASE)
    private val GITLAB = Regex("""gitlab\.com[/:]([^/]+)/([^/.\s?#]+)""", RegexOption.IGNORE_CASE)
    private val CODEBERG = Regex("""codeberg\.org[/:]([^/]+)/([^/.\s?#]+)""", RegexOption.IGNORE_CASE)

    fun parse(raw: String): ForgeRef? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        parseUrl(trimmed)?.let { return it }
        val match = SLASH_FORM.matchEntire(trimmed) ?: return null
        return ForgeRef(
            ReleaseForge.GITHUB,
            "${match.groupValues[1]}/${match.groupValues[2].removeSuffix(".git")}",
        )
    }

    /** Host-bearing URLs / share text only (no bare owner/repo). */
    fun parseUrl(raw: String): ForgeRef? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        return HOST_PATTERNS.firstNotNullOfOrNull { (regex, forge) ->
            regex.find(trimmed)?.let { ForgeRef(forge, path(it)) }
        }
    }

    private fun path(match: MatchResult): String = "${match.groupValues[1]}/${match.groupValues[2].removeSuffix(".git")}"

    private val HOST_PATTERNS = listOf(
        GITHUB to ReleaseForge.GITHUB,
        GITLAB to ReleaseForge.GITLAB,
        CODEBERG to ReleaseForge.CODEBERG,
    )
}

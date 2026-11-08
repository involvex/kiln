package dev.involvex.kiln.feature.sources.github

import dev.involvex.kiln.feature.sources.obtainium.ObtainiumImport

object GitHubOwnerRepo {
    private val SLASH_FORM = Regex("""^([A-Za-z0-9_.-]+)/([A-Za-z0-9_.-]+)$""")

    /** Accepts `owner/repo` or a github.com URL / share text. */
    fun parse(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        ObtainiumImport.githubOwnerRepoFromUrl(trimmed)?.let { return it }
        val match = SLASH_FORM.matchEntire(trimmed) ?: return null
        return "${match.groupValues[1]}/${match.groupValues[2].removeSuffix(".git")}"
    }

    fun provisionalPackageName(ownerRepo: String): String {
        val sanitized = ownerRepo.lowercase()
            .replace('/', '.')
            .filter { it.isLetterOrDigit() || it == '.' || it == '_' }
        return "github.$sanitized"
    }
}

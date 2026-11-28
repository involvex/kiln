package dev.involvex.kiln.feature.sources.forge

import dev.involvex.kiln.core.model.ReleaseForge
import dev.involvex.kiln.core.model.displayLabel
import dev.involvex.kiln.core.model.sourceId

/** Parsed release-forge identity: host + `owner/repo` path. */
data class ForgeRef(val forge: ReleaseForge, val ownerRepo: String) {
    val sourceId: String get() = forge.sourceId(ownerRepo)

    val displayLabel: String get() = "${forge.displayLabel()} $ownerRepo"

    fun provisionalPackageName(): String {
        val prefix = when (forge) {
            ReleaseForge.GITHUB -> "github"
            ReleaseForge.GITLAB -> "gitlab"
            ReleaseForge.CODEBERG -> "codeberg"
        }
        val sanitized = ownerRepo.lowercase()
            .replace('/', '.')
            .filter { it.isLetterOrDigit() || it == '.' || it == '_' }
        return "$prefix.$sanitized"
    }

    /** Query string that [ForgeUrlParse.parse] resolves back to this ref (bare path only for GitHub). */
    fun asLookupQuery(): String = when (forge) {
        ReleaseForge.GITHUB -> ownerRepo
        ReleaseForge.GITLAB -> "https://gitlab.com/$ownerRepo"
        ReleaseForge.CODEBERG -> "https://codeberg.org/$ownerRepo"
    }
}

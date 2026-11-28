package dev.involvex.kiln.feature.sources.github

import dev.involvex.kiln.core.model.ReleaseForge
import dev.involvex.kiln.feature.sources.forge.ForgeRef
import dev.involvex.kiln.feature.sources.forge.ForgeUrlParse

object GitHubOwnerRepo {
    /** Accepts `owner/repo` or a github.com URL / share text (GitHub only). */
    fun parse(raw: String): String? {
        val ref = ForgeUrlParse.parse(raw) ?: return null
        return if (ref.forge == ReleaseForge.GITHUB) ref.ownerRepo else null
    }

    fun provisionalPackageName(ownerRepo: String): String = ForgeRef(ReleaseForge.GITHUB, ownerRepo).provisionalPackageName()
}

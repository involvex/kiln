package dev.involvex.kiln.core.model

/**
 * Encrypted GitLab.com / Codeberg tokens for Settings.
 * GitHub stays on [GitHubTokenRepository]. Never log token values. Bound in `:app`.
 */
interface ForgeTokenRepository {
    fun isTokenSet(forge: ReleaseForge): Boolean

    fun saveToken(forge: ReleaseForge, token: String)

    fun clearToken(forge: ReleaseForge)
}

package dev.involvex.kiln.core.model

/**
 * Encrypted GitHub PAT storage for Settings / Sources UI.
 * Implementations must never log the token. Bound in `:app`.
 */
interface GitHubTokenRepository {
    /** Whether a non-blank PAT is currently stored. */
    fun isTokenSet(): Boolean

    /** Persist [token] (trimmed). Blank clears. */
    fun saveToken(token: String)

    /** Remove any stored PAT. */
    fun clearToken()
}

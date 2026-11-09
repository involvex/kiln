package dev.involvex.kiln.core.model

/**
 * Provides optional GitHub Authorization headers for API calls that require a PAT
 * (Actions artifact downloads). Implementations must never log the token.
 */
interface GitHubAuthHeaderProvider {
    /** Empty map when no PAT is stored. */
    suspend fun bearerHeaders(): Map<String, String>

    /** Whether a non-blank PAT is currently stored. */
    suspend fun hasToken(): Boolean
}

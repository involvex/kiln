package dev.involvex.kiln.core.model

import dev.involvex.kiln.core.common.Result
import kotlinx.coroutines.flow.Flow

/** Public Exodus reports deep-link helpers (no API key). */
object ExodusReportUrls {
    const val DEFAULT_REPORTS_ORIGIN = "https://reports.exodus-privacy.eu.org"
    const val ATTRIBUTION = "Tracker data © Exodus Privacy (ODbL)"

    /**
     * Browser URL for a package report page.
     * @param languageTag BCP-47 language (e.g. `en`, `de`); only `en`/`de` used in path.
     */
    fun reportPageUrl(packageName: String, languageTag: String = "en"): String {
        val lang = if (languageTag.startsWith("de", ignoreCase = true)) "de" else "en"
        val handle = packageName.trim()
        require(handle.isNotEmpty()) { "packageName blank" }
        return "$DEFAULT_REPORTS_ORIGIN/$lang/reports/$handle/"
    }

    /** Normalize a user-entered API origin (trim, no trailing slash). */
    fun normalizeApiBase(raw: String): String = raw.trim().trimEnd('/')
}

data class ExodusPrivacyPrefs(
    /** Self-hosted or allowed Exodus API origin; empty = deep-link only. */
    val apiBaseUrl: String = "",
    val tokenSet: Boolean = false,
) {
    fun isApiConfigured(): Boolean = apiBaseUrl.isNotBlank() && tokenSet
}

interface ExodusPrivacyPreferences {
    fun observe(): Flow<ExodusPrivacyPrefs>

    suspend fun current(): ExodusPrivacyPrefs

    suspend fun setApiBaseUrl(url: String)

    /** Blank clears the token. Never log the value. */
    suspend fun setToken(token: String?)

    /** For network client only — do not expose in UI state. */
    fun peekToken(): String?
}

data class ExodusTrackerReport(val packageName: String, val trackerNames: List<String>, val reportCount: Int)

/**
 * Optional Exodus report lookup. Returns failure when API is not configured
 * (callers should fall back to [ExodusReportUrls.reportPageUrl]).
 */
interface ExodusReportRepository {
    suspend fun reportFor(packageName: String): Result<ExodusTrackerReport>
}

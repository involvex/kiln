package dev.involvex.kiln.feature.sources.exodus

import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.core.model.ExodusPrivacyPreferences
import dev.involvex.kiln.core.model.ExodusReportRepository
import dev.involvex.kiln.core.model.ExodusTrackerReport
import dev.involvex.kiln.core.network.KilnHttpClient
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException
import timber.log.Timber

@Singleton
class DefaultExodusReportRepository @Inject constructor(
    private val http: KilnHttpClient,
    private val preferences: ExodusPrivacyPreferences,
) : ExodusReportRepository {
    private val trackerMutex = Mutex()

    @Volatile
    private var trackerNamesById: Map<Int, String>? = null

    override suspend fun reportFor(packageName: String): Result<ExodusTrackerReport> {
        val prefs = preferences.current()
        val token = preferences.peekToken()
        if (!prefs.isApiConfigured() || token == null) {
            return Result.failure("exodus_not_configured")
        }
        val handle = packageName.trim()
        if (handle.isEmpty()) {
            return Result.failure("invalid_package")
        }
        val base = prefs.apiBaseUrl
        val headers = mapOf(
            "Authorization" to "Token $token",
            "Accept" to "application/json",
            "User-Agent" to USER_AGENT,
        )
        return when (val body = http.getText("$base/api/search/$handle", headers)) {
            is Result.Failure -> body
            is Result.Success -> parseSearchBody(handle, body.value, base, headers)
        }
    }

    private suspend fun parseSearchBody(
        handle: String,
        json: String,
        base: String,
        headers: Map<String, String>,
    ): Result<ExodusTrackerReport> = try {
        val parsed = ExodusSearchParser.parseSearch(handle, json)
        if (parsed == null) {
            Result.failure("exodus_no_report")
        } else {
            val names = resolveTrackerNames(parsed.trackerIds, base, headers)
            Result.success(
                ExodusTrackerReport(
                    packageName = handle,
                    trackerNames = names,
                    reportCount = parsed.reportCount,
                ),
            )
        }
    } catch (error: SerializationException) {
        Timber.w(error, "Exodus search parse failed")
        Result.failure("exodus_parse_failed", error)
    } catch (error: IllegalArgumentException) {
        Result.failure(error.message ?: "exodus_parse_failed", error)
    }

    private suspend fun resolveTrackerNames(ids: List<Int>, base: String, headers: Map<String, String>): List<String> {
        if (ids.isEmpty()) return emptyList()
        val map = ensureTrackerMap(base, headers)
        return ids.map { id -> map[id] ?: "Tracker #$id" }
    }

    private suspend fun ensureTrackerMap(base: String, headers: Map<String, String>): Map<Int, String> {
        trackerNamesById?.let { return it }
        return trackerMutex.withLock {
            trackerNamesById?.let { return it }
            when (val body = http.getText("$base/api/trackers", headers)) {
                is Result.Failure -> {
                    Timber.w("Exodus trackers fetch failed: %s", body.message)
                    emptyMap()
                }

                is Result.Success -> {
                    val map = ExodusSearchParser.parseTrackersMap(body.value)
                    trackerNamesById = map
                    map
                }
            }
        }
    }

    companion object {
        private const val USER_AGENT = "Kiln/0.2.3 (dev.involvex.kiln)"
    }
}

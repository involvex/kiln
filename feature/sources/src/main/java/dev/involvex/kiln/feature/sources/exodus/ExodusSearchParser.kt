package dev.involvex.kiln.feature.sources.exodus

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

/** Pure JSON helpers for Exodus `/api/search` and `/api/trackers`. */
object ExodusSearchParser {
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    data class SearchApp(val name: String? = null, val reports: List<SearchReport> = emptyList())

    @Serializable
    data class SearchReport(
        val trackers: List<Int> = emptyList(),
        @SerialName("version_code") val versionCode: String? = null,
        val version: String? = null,
        val id: Int? = null,
    )

    @Serializable
    data class TrackerDto(val name: String, val categories: List<String> = emptyList())

    fun parseSearch(packageName: String, body: String): ParsedSearch? {
        val root = json.parseToJsonElement(body).jsonObject
        val appElement = root[packageName] ?: return null
        val app = json.decodeFromJsonElement<SearchApp>(appElement)
        val latest = app.reports.maxByOrNull { it.id ?: 0 } ?: app.reports.firstOrNull()
        return ParsedSearch(
            packageName = packageName,
            appName = app.name,
            trackerIds = latest?.trackers.orEmpty(),
            reportCount = app.reports.size,
        )
    }

    fun parseTrackersMap(body: String): Map<Int, String> {
        val root = json.parseToJsonElement(body).jsonObject
        val trackers = root["trackers"] as? JsonObject ?: return emptyMap()
        return trackers.entries.mapNotNull { (id, element) ->
            val dto = runCatching { json.decodeFromJsonElement<TrackerDto>(element) }.getOrNull()
                ?: return@mapNotNull null
            id.toIntOrNull()?.let { it to dto.name }
        }.toMap()
    }

    data class ParsedSearch(val packageName: String, val appName: String?, val trackerIds: List<Int>, val reportCount: Int)
}

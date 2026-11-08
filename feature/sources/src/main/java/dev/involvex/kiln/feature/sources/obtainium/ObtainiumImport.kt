package dev.involvex.kiln.feature.sources.obtainium

import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.core.model.ManagedApp
import dev.involvex.kiln.core.model.MatchConfidence
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Minimal Obtainium export import: expects a JSON object keyed by package name,
 * or an array of objects with `id` / package fields and optional GitHub source URL.
 */
object ObtainiumImport {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun parse(exportJson: String): Result<List<ManagedApp>> = try {
        Result.success(collectApps(json.parseToJsonElement(exportJson)))
    } catch (error: SerializationException) {
        Result.failure(error.message ?: "obtainium_parse_failed", error)
    } catch (error: IllegalArgumentException) {
        Result.failure(error.message ?: "obtainium_parse_failed", error)
    }

    private fun collectApps(root: JsonElement): List<ManagedApp> = when (root) {
        is JsonArray -> root.mapNotNull { el -> (el as? JsonObject)?.let { parseOne(it) } }
        is JsonObject -> collectFromObject(root)
        else -> emptyList()
    }

    private fun collectFromObject(root: JsonObject): List<ManagedApp> {
        val appsEl = root["apps"]
        if (appsEl is JsonArray) {
            return appsEl.mapNotNull { el -> (el as? JsonObject)?.let { parseOne(it) } }
        }
        return root.mapNotNull { (key, value) ->
            (value as? JsonObject)?.let { parseOne(it, fallbackPackage = key) }
        }
    }

    private fun parseOne(obj: JsonObject, fallbackPackage: String? = null): ManagedApp? {
        val packageName = obj["id"]?.jsonPrimitive?.contentOrNull
            ?: obj["packageName"]?.jsonPrimitive?.contentOrNull
            ?: fallbackPackage
            ?: return null
        val additional = obj["additionalSettings"]?.jsonObject
        val url = obj["url"]?.jsonPrimitive?.contentOrNull
            ?: additional?.get("url")?.jsonPrimitive?.contentOrNull
        val ownerRepo = url?.let { githubOwnerRepoFromUrl(it) }
        val sourceId = if (ownerRepo != null) "github:$ownerRepo" else "manual"
        return ManagedApp(
            packageName = packageName,
            sourceId = sourceId,
            confidence = if (ownerRepo != null) MatchConfidence.MEDIUM else MatchConfidence.NONE,
            pinned = false,
            ignoreUpdates = false,
            githubOwnerRepo = ownerRepo,
            githubAssetRegex = ".*\\.apk$",
            includePreReleases = false,
        )
    }

    fun githubOwnerRepoFromUrl(url: String): String? {
        val regex = Regex("""github\.com[/:]([^/]+)/([^/.\s]+)""")
        val match = regex.find(url) ?: return null
        return "${match.groupValues[1]}/${match.groupValues[2].removeSuffix(".git")}"
    }
}

package dev.involvex.kiln.feature.sources.obtainium

import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.core.model.ManagedApp
import dev.involvex.kiln.core.model.MatchConfidence
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Obtainium / Discoverium export import.
 *
 * Accepts:
 * - `{ "schemaVersion": N, "apps": [ … ] }` (current Obtainium/Discoverium)
 * - `{ "apps": [ … ] }` (legacy wrapper)
 * - `[ … ]` bare array
 * - `{ "<package>": { … } }` keyed map
 *
 * `additionalSettings` may be a nested object or a JSON **string** (Obtainium's
 * on-disk form).
 */
object ObtainiumImport {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        allowTrailingComma = true
    }

    fun parse(exportJson: String): Result<List<ManagedApp>> {
        val trimmed = exportJson.trim().removePrefix("\uFEFF")
        if (trimmed.isBlank()) {
            return Result.failure("obtainium_empty")
        }
        if (trimmed.length < MIN_JSON_LENGTH || (!trimmed.startsWith("{") && !trimmed.startsWith("["))) {
            return Result.failure("obtainium_too_short")
        }
        return try {
            val apps = collectApps(json.parseToJsonElement(trimmed))
            if (apps.isEmpty()) {
                Result.failure("obtainium_no_apps")
            } else {
                Result.success(apps)
            }
        } catch (error: SerializationException) {
            Result.failure(error.message ?: "obtainium_parse_failed", error)
        } catch (error: IllegalArgumentException) {
            Result.failure(error.message ?: "obtainium_parse_failed", error)
        }
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
        // Config-site style: { "configs": [ { "id", "url", … } ] }
        val configsEl = root["configs"]
        if (configsEl is JsonArray) {
            return configsEl.mapNotNull { el -> (el as? JsonObject)?.let { parseOne(it) } }
        }
        val single = root["config"]
        if (single is JsonObject) {
            return listOfNotNull(parseOne(single))
        }
        return root.mapNotNull { (key, value) ->
            if (key in META_KEYS) return@mapNotNull null
            (value as? JsonObject)?.let { parseOne(it, fallbackPackage = key) }
        }
    }

    private fun parseOne(obj: JsonObject, fallbackPackage: String? = null): ManagedApp? {
        val packageName = obj["id"]?.jsonPrimitive?.contentOrNull
            ?: obj["packageName"]?.jsonPrimitive?.contentOrNull
            ?: fallbackPackage
            ?: return null
        val additional = parseAdditionalSettings(obj["additionalSettings"])
        val url = obj["url"]?.jsonPrimitive?.contentOrNull
            ?: additional?.get("url")?.jsonPrimitive?.contentOrNull
        val ownerRepo = url?.let { githubOwnerRepoFromUrl(it) }
        val assetRegex = additional?.get("apkFilterRegEx")?.jsonPrimitive?.contentOrNull
            ?.takeIf { it.isNotBlank() }
            ?: ".*\\.apk$"
        val includePre = additional?.get("includePrereleases")?.jsonPrimitive?.contentOrNull
            ?.equals("true", ignoreCase = true) == true
        val sourceId = if (ownerRepo != null) "github:$ownerRepo" else "manual"
        return ManagedApp(
            packageName = packageName,
            sourceId = sourceId,
            confidence = if (ownerRepo != null) MatchConfidence.MEDIUM else MatchConfidence.NONE,
            pinned = false,
            ignoreUpdates = false,
            githubOwnerRepo = ownerRepo,
            githubAssetRegex = assetRegex,
            includePreReleases = includePre,
        )
    }

    private fun parseAdditionalSettings(element: JsonElement?): JsonObject? = when (element) {
        is JsonObject -> element

        is JsonPrimitive -> {
            val raw = element.contentOrNull?.trim().orEmpty()
            if (raw.isEmpty()) {
                null
            } else {
                runCatching { json.parseToJsonElement(raw).jsonObject }.getOrNull()
            }
        }

        else -> null
    }

    fun githubOwnerRepoFromUrl(url: String): String? {
        val regex = Regex("""github\.com[/:]([^/]+)/([^/.\s?#]+)""")
        val match = regex.find(url) ?: return null
        return "${match.groupValues[1]}/${match.groupValues[2].removeSuffix(".git")}"
    }

    private val META_KEYS = setOf(
        "schemaVersion",
        "exportedAt",
        "appVersion",
        "apps",
        "settings",
        "configs",
        "config",
        "icon",
        "categories",
        "description",
    )

    private const val MIN_JSON_LENGTH = 2
}

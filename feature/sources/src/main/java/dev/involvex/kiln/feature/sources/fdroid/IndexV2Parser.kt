package dev.involvex.kiln.feature.sources.fdroid

import dev.involvex.kiln.core.common.Result
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

data class ParsedIndexPackage(
    val packageName: String,
    val name: String,
    val summary: String?,
    val sourceCodeUrl: String?,
    /** F-Droid index-v2 `metadata.categories` (e.g. Internet, System). */
    val categories: List<String> = emptyList(),
    val signerSha256: List<String>,
    val suggestedVersionCode: Long?,
    val suggestedVersionName: String?,
    val releases: List<ParsedIndexRelease>,
)

data class ParsedIndexRelease(
    val versionName: String,
    val versionCode: Long,
    val apkName: String,
    val apkSha256: String?,
    val sizeBytes: Long?,
    val signerSha256: List<String>,
)

data class EntryIndexPointer(val name: String, val sha256: String, val size: Long?)

object IndexV2Parser {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun parseEntryPointer(entryJsonBytes: ByteArray): Result<EntryIndexPointer> = try {
        val root = json.parseToJsonElement(entryJsonBytes.decodeToString()).jsonObject
        val index = root["index"]?.jsonObject
        val name = index?.get("name")?.jsonPrimitive?.contentOrNull
        val sha256 = index?.get("sha256")?.jsonPrimitive?.contentOrNull
        when {
            index == null -> Result.failure("entry_missing_index")

            name == null -> Result.failure("entry_missing_index_name")

            sha256 == null -> Result.failure("entry_missing_index_sha256")

            else -> Result.success(
                EntryIndexPointer(
                    // F-Droid publishes "/index-v2.json"; strip so URL join stays clean.
                    name = name.trimStart('/'),
                    sha256 = sha256,
                    size = index["size"]?.jsonPrimitive?.longOrNull,
                ),
            )
        }
    } catch (error: SerializationException) {
        Result.failure(error.message ?: "entry_parse_failed", error)
    } catch (error: IllegalArgumentException) {
        Result.failure(error.message ?: "entry_parse_failed", error)
    }

    fun parsePackages(indexJson: String, limit: Int = Int.MAX_VALUE): Result<List<ParsedIndexPackage>> {
        return try {
            val root = json.parseToJsonElement(indexJson).jsonObject
            val packagesObj = root["packages"]?.jsonObject
                ?: return Result.success(emptyList())
            val out = ArrayList<ParsedIndexPackage>(minOf(packagesObj.size, limit))
            for ((packageName, pkgElement) in packagesObj) {
                if (out.size >= limit) break
                out += parsePackage(packageName, pkgElement.jsonObject)
            }
            Result.success(out)
        } catch (error: SerializationException) {
            Result.failure(error.message ?: "index_parse_failed", error)
        } catch (error: IllegalArgumentException) {
            Result.failure(error.message ?: "index_parse_failed", error)
        }
    }

    /** Parses a single package object JSON (the value under `packages.<id>`). */
    fun parsePackageJson(packageName: String, packageJson: String): ParsedIndexPackage? = try {
        parsePackage(packageName, json.parseToJsonElement(packageJson).jsonObject)
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }

    private fun parsePackage(packageName: String, pkg: JsonObject): ParsedIndexPackage {
        val metadata = pkg["metadata"]?.jsonObject
        val name = localized(metadata?.get("name")) ?: packageName
        val summary = localized(metadata?.get("summary"))
        val sourceCode = metadata?.get("sourceCode")?.jsonPrimitive?.contentOrNull
        val categories = parseCategories(metadata?.get("categories"))
        val versions = pkg["versions"]?.jsonObject.orEmpty()
        val releases = versions.values.mapNotNull { parseRelease(it) }
            .sortedByDescending { it.versionCode }
        return ParsedIndexPackage(
            packageName = packageName,
            name = name,
            summary = summary,
            sourceCodeUrl = sourceCode,
            categories = categories,
            signerSha256 = releases.firstOrNull()?.signerSha256.orEmpty(),
            suggestedVersionCode = releases.firstOrNull()?.versionCode,
            suggestedVersionName = releases.firstOrNull()?.versionName,
            releases = releases,
        )
    }

    private fun parseCategories(element: JsonElement?): List<String> {
        if (element == null) return emptyList()
        return when (element) {
            is JsonArray -> element.mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.takeIf { c -> c.isNotBlank() } }
            is JsonPrimitive -> element.contentOrNull?.takeIf { it.isNotBlank() }?.let { listOf(it) }.orEmpty()
            else -> emptyList()
        }
    }

    private fun parseRelease(element: JsonElement): ParsedIndexRelease? {
        val obj = element as? JsonObject ?: return null
        val manifest = obj["manifest"]?.jsonObject
        val file = obj["file"]?.jsonObject
        if (manifest == null || file == null) return null
        val versionName = manifest["versionName"]?.jsonPrimitive?.contentOrNull
        val versionCode = manifest["versionCode"]?.jsonPrimitive?.longOrNull
        val apkName = file["name"]?.jsonPrimitive?.contentOrNull
        if (versionName == null || versionCode == null || apkName == null) return null
        val signerList = manifest["signer"]?.jsonObject
            ?.get("sha256")
            ?.jsonArray
            ?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
            .orEmpty()
        return ParsedIndexRelease(
            versionName = versionName,
            versionCode = versionCode,
            apkName = apkName,
            apkSha256 = file["sha256"]?.jsonPrimitive?.contentOrNull,
            sizeBytes = file["size"]?.jsonPrimitive?.longOrNull,
            signerSha256 = signerList,
        )
    }

    private fun localized(element: JsonElement?): String? {
        if (element == null) return null
        return when (element) {
            is JsonPrimitive -> element.contentOrNull

            is JsonObject -> {
                element["en-US"]?.jsonPrimitive?.contentOrNull
                    ?: element["en"]?.jsonPrimitive?.contentOrNull
                    ?: element.values.firstOrNull()?.let { localized(it) }
            }

            is JsonArray -> element.firstOrNull()?.let { localized(it) }
        }
    }
}

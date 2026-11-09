package dev.involvex.kiln.feature.settings.config

import dev.involvex.kiln.core.common.Result
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object KilnConfigCodec {
    const val SCHEMA_VERSION = 1

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    fun encode(document: KilnConfigV1): String = json.encodeToString(document)

    fun decode(raw: String): Result<KilnConfigV1> {
        val trimmed = raw.trim().removePrefix("\uFEFF")
        if (trimmed.isBlank()) {
            return Result.failure("kiln_config_empty")
        }
        if (!trimmed.startsWith("{")) {
            return Result.failure("kiln_config_not_object")
        }
        return try {
            val doc = json.decodeFromString<KilnConfigV1>(trimmed)
            if (doc.schemaVersion != SCHEMA_VERSION) {
                Result.failure("kiln_config_unsupported_schema:${doc.schemaVersion}")
            } else {
                Result.success(doc)
            }
        } catch (error: SerializationException) {
            Result.failure(error.message ?: "kiln_config_parse_failed", error)
        } catch (error: IllegalArgumentException) {
            Result.failure(error.message ?: "kiln_config_parse_failed", error)
        }
    }

    /** True when the JSON text looks like it might contain secret fields (defense-in-depth audit). */
    fun containsForbiddenSecretKeys(raw: String): Boolean {
        val lower = raw.lowercase()
        return FORBIDDEN_KEY_MARKERS.any { lower.contains(it) }
    }

    private val FORBIDDEN_KEY_MARKERS = listOf(
        "\"pat\"",
        "\"token\"",
        "\"github_pat\"",
        "\"bearer\"",
        "\"password\"",
        "\"api_key\"",
        "\"apikey\"",
    )
}

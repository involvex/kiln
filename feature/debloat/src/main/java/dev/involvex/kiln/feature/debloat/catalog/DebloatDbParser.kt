package dev.involvex.kiln.feature.debloat.catalog

import dev.involvex.kiln.core.model.DebloatCatalogEntry
import dev.involvex.kiln.core.model.DebloatRating
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Parsed curated debloat database (asset or fixture). Ratings are never invented here. */
data class DebloatDbDocument(val version: Int, val source: String?, val notes: String?, val entries: Map<String, DebloatCatalogEntry>) {
    val packageCount: Int get() = entries.size
}

/**
 * Pure JSON → [DebloatDbDocument] parser (unit-tested).
 * Invalid rating strings map to [DebloatRating.UNKNOWN].
 */
object DebloatDbParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(text: String): DebloatDbDocument {
        val file = json.decodeFromString(DebloatDbFile.serializer(), text)
        val entries = file.packages.associate { raw ->
            val rating = runCatching { DebloatRating.valueOf(raw.rating) }
                .getOrDefault(DebloatRating.UNKNOWN)
            raw.pkg to DebloatCatalogEntry(
                packageName = raw.pkg,
                label = raw.label,
                vendor = raw.vendor,
                category = raw.category,
                rating = rating,
                description = raw.description,
                breaks = raw.breaks.orEmpty(),
                minSdk = raw.minSdk ?: 0,
                maxSdk = raw.maxSdk ?: 99,
            )
        }
        return DebloatDbDocument(
            version = file.version,
            source = file.source,
            notes = file.notes,
            entries = entries,
        )
    }

    @Serializable
    internal data class DebloatDbFile(
        val version: Int = 1,
        val source: String? = null,
        val notes: String? = null,
        val packages: List<DebloatDbPackage> = emptyList(),
    )

    @Serializable
    internal data class DebloatDbPackage(
        val pkg: String,
        val label: String? = null,
        val vendor: String? = null,
        val category: String? = null,
        val rating: String = "UNKNOWN",
        val description: String? = null,
        val breaks: List<String>? = null,
        val minSdk: Int? = null,
        val maxSdk: Int? = null,
    )
}

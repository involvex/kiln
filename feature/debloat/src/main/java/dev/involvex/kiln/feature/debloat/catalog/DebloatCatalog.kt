package dev.involvex.kiln.feature.debloat.catalog

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.involvex.kiln.core.model.DebloatCatalogEntry
import dev.involvex.kiln.core.model.DebloatRating
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber

@Singleton
class DebloatCatalog @Inject constructor(@ApplicationContext private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    private var cached: Map<String, DebloatCatalogEntry>? = null

    fun entryFor(packageName: String): DebloatCatalogEntry? = load()[packageName]

    fun ratingFor(packageName: String): DebloatRating = entryFor(packageName)?.rating ?: DebloatRating.UNKNOWN

    fun load(): Map<String, DebloatCatalogEntry> {
        cached?.let { return it }
        return synchronized(this) {
            cached ?: readAsset().also { cached = it }
        }
    }

    private fun readAsset(): Map<String, DebloatCatalogEntry> = try {
        val text = context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
        val file = json.decodeFromString(DebloatDbFile.serializer(), text)
        file.packages.associate { raw ->
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
    } catch (e: Exception) {
        Timber.e(e, "Failed to load debloat-db.json")
        emptyMap()
    }

    @Serializable
    private data class DebloatDbFile(
        val version: Int = 1,
        val source: String? = null,
        val notes: String? = null,
        val packages: List<DebloatDbPackage> = emptyList(),
    )

    @Serializable
    private data class DebloatDbPackage(
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

    companion object {
        const val ASSET_NAME = "debloat-db.json"
    }
}

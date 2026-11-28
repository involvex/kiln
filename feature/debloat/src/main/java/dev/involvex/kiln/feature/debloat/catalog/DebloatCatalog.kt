package dev.involvex.kiln.feature.debloat.catalog

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.involvex.kiln.core.model.DebloatCatalogEntry
import dev.involvex.kiln.core.model.DebloatRating
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class DebloatCatalog @Inject constructor(@ApplicationContext private val context: Context) {
    @Volatile
    private var cached: DebloatDbDocument? = null

    fun document(): DebloatDbDocument = loadDocument()

    fun entryFor(packageName: String): DebloatCatalogEntry? = loadDocument().entries[packageName]

    fun ratingFor(packageName: String): DebloatRating = entryFor(packageName)?.rating ?: DebloatRating.UNKNOWN

    fun load(): Map<String, DebloatCatalogEntry> = loadDocument().entries

    private fun loadDocument(): DebloatDbDocument {
        cached?.let { return it }
        return synchronized(this) {
            cached ?: readAsset().also { cached = it }
        }
    }

    private fun readAsset(): DebloatDbDocument = try {
        val text = context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
        DebloatDbParser.parse(text)
    } catch (e: Exception) {
        Timber.e(e, "Failed to load debloat-db.json")
        DebloatDbDocument(version = 0, source = null, notes = null, entries = emptyMap())
    }

    companion object {
        const val ASSET_NAME = "debloat-db.json"
    }
}

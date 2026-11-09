package dev.involvex.kiln.feature.sources.discovery

import dev.involvex.kiln.core.database.IndexDao
import dev.involvex.kiln.core.database.IndexedPackageEntity
import dev.involvex.kiln.core.database.RepoDao
import dev.involvex.kiln.core.database.RepoEntity
import javax.inject.Inject
import javax.inject.Singleton

enum class TrendingShelfId {
    PRIVACY,
    INTERNET,
    MEDIA,
    SYSTEM,
    AI,
}

data class TrendingShelf(
    val id: TrendingShelfId,
    val apps: List<DiscoveryAppRow>,
)

/**
 * Topic shelves are **heuristic**, not popularity charts: apps come from the local F-Droid-style
 * catalog filtered by [IndexedPackageEntity.categoriesCsv] and simple name/summary keyword rules
 * (e.g. AI-related). Within each shelf, ordering uses [IndexedPackageEntity.suggestedVersionCode]
 * descending as a stand-in for “recent catalog activity” — Kiln does not collect or use download counts.
 */
@Singleton
class TrendingRepository @Inject constructor(
    private val indexDao: IndexDao,
    private val repoDao: RepoDao,
) {
    suspend fun loadShelves(): List<TrendingShelf> {
        val repos = repoDao.getAll().associateBy { it.id }
        return listOf(
            shelfPrivacy(repos),
            shelfCategory(TrendingShelfId.INTERNET, "Internet", repos),
            shelfCategories(TrendingShelfId.MEDIA, listOf("Multimedia", "Graphics"), repos),
            shelfCategories(TrendingShelfId.SYSTEM, listOf("System", "Development"), repos),
            shelfAi(repos),
        ).filter { it.apps.isNotEmpty() }
    }

    private suspend fun shelfPrivacy(repos: Map<String, RepoEntity>): TrendingShelf {
        val security = indexDao.packagesByCategory("Security", SHELF_QUERY_LIMIT)
        val internet = indexDao.packagesByCategory("Internet", SHELF_QUERY_LIMIT)
        val merged = linkedMapOf<Pair<String, String>, IndexedPackageEntity>()
        for (entity in security + internet) {
            merged[entity.packageName to entity.repoId] = entity
        }
        val apps = merged.values
            .sortedByDescending { it.suggestedVersionCode ?: 0L }
            .take(SHELF_DISPLAY_LIMIT)
            .map { it.toDiscoveryRow(repos[it.repoId]) }
        return TrendingShelf(TrendingShelfId.PRIVACY, apps)
    }

    private suspend fun shelfCategory(
        id: TrendingShelfId,
        category: String,
        repos: Map<String, RepoEntity>,
    ): TrendingShelf {
        val apps = indexDao.packagesByCategory(category, SHELF_DISPLAY_LIMIT)
            .map { it.toDiscoveryRow(repos[it.repoId]) }
        return TrendingShelf(id, apps)
    }

    private suspend fun shelfCategories(
        id: TrendingShelfId,
        categories: List<String>,
        repos: Map<String, RepoEntity>,
    ): TrendingShelf {
        val merged = linkedMapOf<Pair<String, String>, IndexedPackageEntity>()
        for (category in categories) {
            for (entity in indexDao.packagesByCategory(category, SHELF_QUERY_LIMIT)) {
                merged[entity.packageName to entity.repoId] = entity
            }
        }
        val apps = merged.values
            .sortedByDescending { it.suggestedVersionCode ?: 0L }
            .take(SHELF_DISPLAY_LIMIT)
            .map { it.toDiscoveryRow(repos[it.repoId]) }
        return TrendingShelf(id, apps)
    }

    private suspend fun shelfAi(repos: Map<String, RepoEntity>): TrendingShelf {
        val candidates = indexDao.packagesForTrending(AI_SCAN_LIMIT)
        val apps = candidates
            .filter { matchesAiHeuristic(it.name, it.summary) }
            .take(SHELF_DISPLAY_LIMIT)
            .map { it.toDiscoveryRow(repos[it.repoId]) }
        return TrendingShelf(TrendingShelfId.AI, apps)
    }

    private fun matchesAiHeuristic(name: String, summary: String?): Boolean {
        val haystack = listOfNotNull(name, summary).joinToString(" ")
        return AI_PATTERN.containsMatchIn(haystack)
    }

    private companion object {
        const val SHELF_DISPLAY_LIMIT = 12
        const val SHELF_QUERY_LIMIT = 48
        const val AI_SCAN_LIMIT = 400
        val AI_PATTERN = Regex(
            """(?i)(\bai\b|\bllm\b|\bgpt\b|machine\s+learning|neural\s+network|openai)""",
        )
    }
}

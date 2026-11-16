package dev.involvex.kiln.feature.sources.discovery

import dev.involvex.kiln.core.database.IndexDao
import dev.involvex.kiln.core.database.IndexedPackageEntity
import dev.involvex.kiln.core.database.RepoDao
import dev.involvex.kiln.core.database.RepoEntity
import javax.inject.Inject
import javax.inject.Singleton

enum class TrendingShelfId {
    RECENTLY_UPDATED,
    NEW_IN_CATALOG,
    PRIVACY,
    INTERNET,
    MEDIA,
    SYSTEM,
    AI,
}

data class TrendingShelf(val id: TrendingShelfId, val apps: List<DiscoveryAppRow>)

/**
 * Discover home shelves are **heuristic**, not popularity charts: local F-Droid catalog
 * filtered by categories / keywords, or ordered by version code / insert row id.
 * Kiln does not collect or use download counts.
 */
@Singleton
class TrendingRepository @Inject constructor(private val indexDao: IndexDao, private val repoDao: RepoDao) {
    suspend fun loadShelves(): List<TrendingShelf> {
        val repos = repoDao.getAll().associateBy { it.id }
        return listOf(
            shelfRecentlyUpdated(repos),
            shelfNewInCatalog(repos),
            shelfPrivacy(repos),
            shelfCategory(TrendingShelfId.INTERNET, "Internet", repos),
            shelfCategories(TrendingShelfId.MEDIA, listOf("Multimedia", "Graphics"), repos),
            shelfCategories(TrendingShelfId.SYSTEM, listOf("System", "Development"), repos),
            shelfAi(repos),
        ).filter { it.apps.isNotEmpty() }
    }

    private suspend fun shelfRecentlyUpdated(repos: Map<String, RepoEntity>): TrendingShelf {
        val apps = indexDao.packagesForTrending(SHELF_DISPLAY_LIMIT)
            .map { it.toDiscoveryRow(repos[it.repoId]) }
        return TrendingShelf(TrendingShelfId.RECENTLY_UPDATED, apps)
    }

    private suspend fun shelfNewInCatalog(repos: Map<String, RepoEntity>): TrendingShelf {
        val apps = indexDao.packagesByNewestRowId(SHELF_DISPLAY_LIMIT)
            .map { it.toDiscoveryRow(repos[it.repoId]) }
        return TrendingShelf(TrendingShelfId.NEW_IN_CATALOG, apps)
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

    private suspend fun shelfCategory(id: TrendingShelfId, category: String, repos: Map<String, RepoEntity>): TrendingShelf {
        val apps = indexDao.packagesByCategory(category, SHELF_DISPLAY_LIMIT)
            .map { it.toDiscoveryRow(repos[it.repoId]) }
        return TrendingShelf(id, apps)
    }

    private suspend fun shelfCategories(id: TrendingShelfId, categories: List<String>, repos: Map<String, RepoEntity>): TrendingShelf {
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
            .filter { DiscoverShelfHeuristics.matchesAi(it.name, it.summary) }
            .take(SHELF_DISPLAY_LIMIT)
            .map { it.toDiscoveryRow(repos[it.repoId]) }
        return TrendingShelf(TrendingShelfId.AI, apps)
    }

    private companion object {
        const val SHELF_DISPLAY_LIMIT = 12
        const val SHELF_QUERY_LIMIT = 48
        const val AI_SCAN_LIMIT = 400
    }
}

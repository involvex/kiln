package dev.involvex.kiln.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface RepoDao {
    @Query("SELECT * FROM repos ORDER BY priority ASC, name ASC")
    fun observeAll(): Flow<List<RepoEntity>>

    @Query("SELECT * FROM repos ORDER BY priority ASC, name ASC")
    suspend fun getAll(): List<RepoEntity>

    @Query("SELECT * FROM repos WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): RepoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(repo: RepoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(repos: List<RepoEntity>)

    @Query("UPDATE repos SET lastSyncedAtEpochMs = :epochMs WHERE id = :id")
    suspend fun updateLastSynced(id: String, epochMs: Long)

    @Query("DELETE FROM repos WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface IndexDao {
    @Query("DELETE FROM indexed_packages WHERE repoId = :repoId")
    suspend fun clearPackages(repoId: String)

    @Query("DELETE FROM indexed_releases WHERE repoId = :repoId")
    suspend fun clearReleases(repoId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPackages(items: List<IndexedPackageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReleases(items: List<IndexedReleaseEntity>)

    @Transaction
    suspend fun replaceRepoIndex(repoId: String, packages: List<IndexedPackageEntity>, releases: List<IndexedReleaseEntity>) {
        clearPackages(repoId)
        clearReleases(repoId)
        if (packages.isNotEmpty()) insertPackages(packages)
        if (releases.isNotEmpty()) insertReleases(releases)
    }

    @Query("SELECT * FROM indexed_packages WHERE packageName = :packageName")
    suspend fun packagesFor(packageName: String): List<IndexedPackageEntity>

    @Query("SELECT * FROM indexed_packages")
    suspend fun allPackages(): List<IndexedPackageEntity>

    @Query(
        """
        SELECT * FROM indexed_releases
        WHERE packageName = :packageName
        ORDER BY versionCode DESC
        """,
    )
    suspend fun releasesFor(packageName: String): List<IndexedReleaseEntity>

    @Query(
        """
        SELECT * FROM indexed_releases
        WHERE packageName = :packageName AND repoId = :repoId
        ORDER BY versionCode DESC
        LIMIT 1
        """,
    )
    suspend fun latestRelease(packageName: String, repoId: String): IndexedReleaseEntity?
}

@Dao
interface ManagedAppDao {
    @Query("SELECT * FROM managed_apps")
    fun observeAll(): Flow<List<ManagedAppEntity>>

    @Query("SELECT * FROM managed_apps")
    suspend fun getAll(): List<ManagedAppEntity>

    @Query("SELECT * FROM managed_apps WHERE packageName = :packageName LIMIT 1")
    suspend fun get(packageName: String): ManagedAppEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(app: ManagedAppEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(apps: List<ManagedAppEntity>)

    @Query("DELETE FROM managed_apps WHERE packageName = :packageName")
    suspend fun delete(packageName: String)
}

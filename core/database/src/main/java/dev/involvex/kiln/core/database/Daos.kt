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
@Suppress("TooManyFunctions") // Room index queries + catalog search/upsert stay on one DAO.
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

    @Query("SELECT COUNT(*) FROM indexed_packages")
    suspend fun packageCount(): Int

    @Query("SELECT DISTINCT categoriesCsv FROM indexed_packages WHERE categoriesCsv != ''")
    suspend fun categoryCsvRows(): List<String>

    @Query(
        """
        SELECT * FROM indexed_packages
        WHERE (:query = '' OR name LIKE '%' || :query || '%'
            OR packageName LIKE '%' || :query || '%'
            OR IFNULL(summary, '') LIKE '%' || :query || '%')
          AND (:category = '' OR categoriesCsv LIKE '%' || :category || '%')
        ORDER BY name COLLATE NOCASE ASC
        LIMIT :limit
        """,
    )
    suspend fun searchPackages(query: String, category: String, limit: Int): List<IndexedPackageEntity>

    @Query(
        """
        DELETE FROM indexed_packages
        WHERE repoId = :repoId AND packageName IN (:packageNames)
        """,
    )
    suspend fun deletePackages(repoId: String, packageNames: List<String>)

    @Query(
        """
        DELETE FROM indexed_releases
        WHERE repoId = :repoId AND packageName IN (:packageNames)
        """,
    )
    suspend fun deleteReleases(repoId: String, packageNames: List<String>)

    @Transaction
    suspend fun upsertPackagesForRepo(repoId: String, packages: List<IndexedPackageEntity>, releases: List<IndexedReleaseEntity>) {
        if (packages.isEmpty()) return
        val names = packages.map { it.packageName }
        deletePackages(repoId, names)
        deleteReleases(repoId, names)
        insertPackages(packages)
        if (releases.isNotEmpty()) insertReleases(releases)
    }

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

    @Query("SELECT * FROM managed_apps WHERE packageName = :packageName LIMIT 1")
    fun observe(packageName: String): Flow<ManagedAppEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(app: ManagedAppEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(apps: List<ManagedAppEntity>)

    @Query("DELETE FROM managed_apps WHERE packageName = :packageName")
    suspend fun delete(packageName: String)
}

@Dao
interface InstallJobDao {
    @Query("SELECT * FROM install_jobs ORDER BY createdAtEpochMs DESC")
    fun observeAll(): Flow<List<InstallJobEntity>>

    @Query("SELECT * FROM install_jobs WHERE jobId = :jobId LIMIT 1")
    suspend fun get(jobId: String): InstallJobEntity?

    @Query("SELECT * FROM install_jobs WHERE packageName = :packageName ORDER BY createdAtEpochMs DESC LIMIT 1")
    suspend fun latestForPackage(packageName: String): InstallJobEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(job: InstallJobEntity)

    @Query(
        """
        UPDATE install_jobs
        SET status = :status, progressPercent = :progressPercent,
            errorMessage = :errorMessage, localApkPath = :localApkPath,
            updatedAtEpochMs = :updatedAtEpochMs
        WHERE jobId = :jobId
        """,
    )
    suspend fun updateProgress(
        jobId: String,
        status: String,
        progressPercent: Int,
        errorMessage: String?,
        localApkPath: String?,
        updatedAtEpochMs: Long,
    )

    @Query("DELETE FROM install_jobs WHERE jobId = :jobId")
    suspend fun delete(jobId: String)
}

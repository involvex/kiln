package dev.involvex.kiln.feature.storage.data

import android.app.usage.StorageStats
import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Process
import android.os.UserHandle
import android.os.storage.StorageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.involvex.kiln.core.common.IoDispatcher
import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.core.model.AppStorageStats
import dev.involvex.kiln.core.model.StorageOverview
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import timber.log.Timber

@Singleton
class StorageStatsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val usageAccessChecker: UsageAccessChecker,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    suspend fun loadOverview(includeSystem: Boolean): Result<StorageOverview> = withContext(ioDispatcher) {
        if (!usageAccessChecker.hasUsageAccess()) {
            return@withContext Result.failure("usage_access_required")
        }
        val pm = context.packageManager
        val statsManager = context.getSystemService(StorageStatsManager::class.java)
            ?: return@withContext Result.failure("storage_stats_unavailable")
        val uuid = storageUuid()
        val user = Process.myUserHandle()
        val packages = pm.getInstalledApplications(PackageManager.MATCH_DISABLED_COMPONENTS)
            .filter { includeSystem || (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
        val rows = packages.mapNotNull { appInfo ->
            queryAppStats(statsManager, pm, uuid, user, appInfo)
        }.sortedByDescending { it.totalBytes }
        Result.success(
            StorageOverview(
                apps = rows,
                totalAppBytes = rows.sumOf { it.appBytes },
                totalDataBytes = rows.sumOf { it.dataBytes },
                totalCacheBytes = rows.sumOf { it.cacheBytes },
            ),
        )
    }

    private fun queryAppStats(
        statsManager: StorageStatsManager,
        pm: PackageManager,
        uuid: UUID,
        user: UserHandle,
        appInfo: ApplicationInfo,
    ): AppStorageStats? {
        val packageName = appInfo.packageName
        val stats = queryStatsOrNull(statsManager, uuid, packageName, user) ?: return null
        val label = try {
            pm.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            packageName
        }
        val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        return AppStorageStats(
            packageName = packageName,
            label = label,
            isSystem = isSystem,
            appBytes = stats.appBytes,
            dataBytes = stats.dataBytes,
            cacheBytes = stats.cacheBytes,
        )
    }

    private fun queryStatsOrNull(statsManager: StorageStatsManager, uuid: UUID, packageName: String, user: UserHandle): StorageStats? =
        try {
            statsManager.queryStatsForPackage(uuid, packageName, user)
        } catch (e: PackageManager.NameNotFoundException) {
            Timber.d(e, "Package gone while measuring %s", packageName)
            null
        } catch (e: IOException) {
            Timber.w(e, "StorageStats failed for %s", packageName)
            null
        } catch (e: SecurityException) {
            Timber.w(e, "StorageStats denied for %s", packageName)
            null
        }

    private fun storageUuid(): UUID {
        val sm = context.getSystemService(StorageManager::class.java) ?: return StorageManager.UUID_DEFAULT
        val dir = context.filesDir ?: return StorageManager.UUID_DEFAULT
        return try {
            sm.getUuidForPath(dir)
        } catch (e: IllegalArgumentException) {
            Timber.w(e, "Falling back to UUID_DEFAULT for storage stats")
            StorageManager.UUID_DEFAULT
        }
    }
}

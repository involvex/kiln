package dev.involvex.kiln.feature.storage.data

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.involvex.kiln.core.common.IoDispatcher
import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.core.model.ActivityLogEvents
import dev.involvex.kiln.core.model.ActivityLogRecorder
import dev.involvex.kiln.core.model.ActivityLogType
import dev.involvex.kiln.core.model.AndroidSharedRoot
import dev.involvex.kiln.core.model.OrphanStorageDir
import dev.involvex.kiln.core.model.PrivilegedExecutor
import dev.involvex.kiln.core.model.StorageOrphanGate
import dev.involvex.kiln.core.model.StoragePhase2Filter
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

@Singleton
class OrphanStorageRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val privilegedExecutor: PrivilegedExecutor,
    private val activityLog: ActivityLogRecorder,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    suspend fun loadOrphans(): Result<List<OrphanStorageDir>> = withContext(ioDispatcher) {
        if (!StorageOrphanGate.canAccessAndroidData(privilegedExecutor.currentStatus().capabilities)) {
            return@withContext Result.failure("capability_missing:ACCESS_ANDROID_DATA")
        }
        val installed = installedPackageNames()
        val orphans = ArrayList<OrphanStorageDir>()
        var anySuccess = false
        var lastFailure: String? = null
        for (root in AndroidSharedRoot.entries) {
            when (val listed = privilegedExecutor.listAndroidSharedPackageDirs(root)) {
                is Result.Failure -> lastFailure = listed.message

                is Result.Success -> {
                    anySuccess = true
                    val packages = StoragePhase2Filter.orphanPackageDirs(listed.value, installed)
                    for (pkg in packages) {
                        val bytes = when (val measured = privilegedExecutor.measureAndroidSharedPackageDir(root, pkg)) {
                            is Result.Success -> measured.value
                            is Result.Failure -> 0L
                        }
                        orphans += OrphanStorageDir(root = root, packageName = pkg, sizeBytes = bytes)
                    }
                }
            }
        }
        if (!anySuccess) {
            return@withContext Result.failure(lastFailure ?: "orphan_scan_failed")
        }
        Result.success(orphans.sortedByDescending { it.sizeBytes })
    }

    suspend fun delete(orphans: List<OrphanStorageDir>): List<Pair<OrphanStorageDir, Boolean>> = withContext(ioDispatcher) {
        if (!StorageOrphanGate.canAccessAndroidData(privilegedExecutor.currentStatus().capabilities)) {
            return@withContext orphans.map { it to false }
        }
        orphans.map { orphan ->
            when (
                val result = privilegedExecutor.deleteAndroidSharedPackageDir(
                    orphan.root,
                    orphan.packageName,
                )
            ) {
                is Result.Success -> {
                    activityLog.record(
                        ActivityLogEvents.create(
                            type = ActivityLogType.CACHE,
                            action = "orphan_delete_${orphan.root.pathKey}",
                            success = true,
                            packageName = orphan.packageName,
                        ),
                    )
                    orphan to true
                }

                is Result.Failure -> {
                    activityLog.record(
                        ActivityLogEvents.create(
                            type = ActivityLogType.CACHE,
                            action = "orphan_delete_${orphan.root.pathKey}",
                            success = false,
                            packageName = orphan.packageName,
                            detailKey = result.message,
                        ),
                    )
                    orphan to false
                }
            }
        }
    }

    private fun installedPackageNames(): Set<String> {
        val pm = context.packageManager
        val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledPackages(0)
        }
        return packages.map { it.packageName }.toSet()
    }
}

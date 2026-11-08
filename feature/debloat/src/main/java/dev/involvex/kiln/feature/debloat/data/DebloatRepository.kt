package dev.involvex.kiln.feature.debloat.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.involvex.kiln.core.common.IoDispatcher
import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.core.model.DebloatAction
import dev.involvex.kiln.core.model.DebloatActionResult
import dev.involvex.kiln.core.model.DebloatAppRow
import dev.involvex.kiln.core.model.DebloatPackageState
import dev.involvex.kiln.core.model.DebloatSnapshot
import dev.involvex.kiln.core.model.DebloatSnapshotPackage
import dev.involvex.kiln.core.model.PrivilegeCapability
import dev.involvex.kiln.core.model.PrivilegedExecutor
import dev.involvex.kiln.feature.debloat.catalog.DebloatCatalog
import dev.involvex.kiln.feature.debloat.denylist.DebloatDenylistResolver
import dev.involvex.kiln.feature.debloat.snapshot.DebloatSnapshotStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import timber.log.Timber

@Singleton
class DebloatRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val catalog: DebloatCatalog,
    private val denylistResolver: DebloatDenylistResolver,
    private val snapshotStore: DebloatSnapshotStore,
    private val privilegedExecutor: PrivilegedExecutor,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    suspend fun loadRows(): List<DebloatAppRow> = withContext(ioDispatcher) {
        val pm = context.packageManager
        val denylist = denylistResolver.resolve()
        val flags = PackageManager.MATCH_DISABLED_COMPONENTS or
            PackageManager.MATCH_UNINSTALLED_PACKAGES
        val apps = pm.getInstalledApplications(flags)
        apps.map { info ->
            val pkg = info.packageName
            val entry = catalog.entryFor(pkg)
            val label = try {
                pm.getApplicationLabel(info).toString()
            } catch (_: Exception) {
                entry?.label ?: pkg
            }
            val state = resolveState(info)
            val reason = denylist[pkg]
            DebloatAppRow(
                packageName = pkg,
                label = label,
                isSystem = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                state = state,
                rating = catalog.ratingFor(pkg),
                category = entry?.category,
                description = entry?.description,
                breaks = entry?.breaks.orEmpty(),
                denylisted = reason != null,
                denylistReason = reason,
            )
        }.sortedBy { it.label.lowercase() }
    }

    suspend fun ensureSnapshot(): Result<DebloatSnapshot> = withContext(ioDispatcher) {
        snapshotStore.load()?.let { return@withContext Result.success(it) }
        val rows = loadRows()
        val snapshot = DebloatSnapshot(
            createdAtEpochMs = System.currentTimeMillis(),
            packages = rows.map { DebloatSnapshotPackage(it.packageName, it.state) },
        )
        when (val saved = snapshotStore.save(snapshot)) {
            is Result.Failure -> saved
            is Result.Success -> Result.success(snapshot)
        }
    }

    suspend fun currentSnapshot(): DebloatSnapshot? = snapshotStore.load()

    suspend fun applyActions(targets: List<Pair<String, DebloatAction>>): List<DebloatActionResult> = withContext(ioDispatcher) {
        if (PrivilegeCapability.DEBLOAT !in privilegedExecutor.currentStatus().capabilities) {
            return@withContext targets.map {
                DebloatActionResult(it.first, it.second, false, "capability_missing:DEBLOAT")
            }
        }
        ensureSnapshot()
        val denylist = denylistResolver.resolve()
        targets.map { (pkg, action) ->
            if (denylist.containsKey(pkg)) {
                return@map DebloatActionResult(pkg, action, false, "denylisted")
            }
            val result = when (action) {
                DebloatAction.DISABLE -> privilegedExecutor.disableUserPackage(pkg)
                DebloatAction.ENABLE -> privilegedExecutor.enablePackage(pkg)
                DebloatAction.UNINSTALL_FOR_USER -> privilegedExecutor.uninstall(pkg, keepData = false)
                DebloatAction.INSTALL_EXISTING -> privilegedExecutor.installExistingPackage(pkg)
            }
            when (result) {
                is Result.Failure -> {
                    Timber.w("Debloat %s %s failed: %s", action, pkg, result.message)
                    DebloatActionResult(pkg, action, false, result.message)
                }

                is Result.Success -> DebloatActionResult(pkg, action, true, null)
            }
        }
    }

    suspend fun restoreSnapshot(): List<DebloatActionResult> = withContext(ioDispatcher) {
        val snapshot = snapshotStore.load()
            ?: return@withContext emptyList()
        val current = loadRows().associateBy { it.packageName }
        val plan = ArrayList<Pair<String, DebloatAction>>()
        for (entry in snapshot.packages) {
            val now = current[entry.packageName] ?: continue
            if (now.denylisted) continue
            if (now.state == entry.state) continue
            val action = when (entry.state) {
                DebloatPackageState.ENABLED -> when (now.state) {
                    DebloatPackageState.DISABLED -> DebloatAction.ENABLE
                    DebloatPackageState.UNINSTALLED_FOR_USER -> DebloatAction.INSTALL_EXISTING
                    DebloatPackageState.ENABLED -> null
                }

                DebloatPackageState.DISABLED -> when (now.state) {
                    DebloatPackageState.ENABLED -> DebloatAction.DISABLE
                    DebloatPackageState.UNINSTALLED_FOR_USER -> DebloatAction.INSTALL_EXISTING
                    DebloatPackageState.DISABLED -> null
                }

                DebloatPackageState.UNINSTALLED_FOR_USER -> when (now.state) {
                    DebloatPackageState.ENABLED,
                    DebloatPackageState.DISABLED,
                    -> DebloatAction.UNINSTALL_FOR_USER

                    DebloatPackageState.UNINSTALLED_FOR_USER -> null
                }
            }
            if (action != null) plan.add(entry.packageName to action)
        }
        applyActions(plan)
    }

    private fun resolveState(info: ApplicationInfo): DebloatPackageState {
        val installed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // FLAG_INSTALLED: API 24+
            (info.flags and ApplicationInfo.FLAG_INSTALLED) != 0
        } else {
            true
        }
        if (!installed) return DebloatPackageState.UNINSTALLED_FOR_USER
        return if (info.enabled) DebloatPackageState.ENABLED else DebloatPackageState.DISABLED
    }
}

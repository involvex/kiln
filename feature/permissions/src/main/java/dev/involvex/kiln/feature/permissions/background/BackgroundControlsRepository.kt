package dev.involvex.kiln.feature.permissions.background

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.involvex.kiln.core.common.IoDispatcher
import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.core.model.ActivityLogEvents
import dev.involvex.kiln.core.model.ActivityLogRecorder
import dev.involvex.kiln.core.model.ActivityLogType
import dev.involvex.kiln.core.model.AppOpOutputParser
import dev.involvex.kiln.core.model.BackgroundControlsApp
import dev.involvex.kiln.core.model.BackgroundControlsGate
import dev.involvex.kiln.core.model.BackgroundControlsOps
import dev.involvex.kiln.core.model.BatteryUsageMode
import dev.involvex.kiln.core.model.BatteryUsageModeMapper
import dev.involvex.kiln.core.model.DeviceIdleWhitelistParser
import dev.involvex.kiln.core.model.PrivilegeCapability
import dev.involvex.kiln.core.model.PrivilegedExecutor
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import timber.log.Timber

@Singleton
class BackgroundControlsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val privilegedExecutor: PrivilegedExecutor,
    private val activityLog: ActivityLogRecorder,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    fun isHyperOs(): Boolean = HyperOsDetector.isHyperOsOrMiui()

    fun canMutate(): Boolean = BackgroundControlsGate.canMutate(privilegedExecutor.currentStatus().capabilities)

    suspend fun loadApps(includeSystem: Boolean): List<BackgroundControlsApp> = withContext(ioDispatcher) {
        val pm = context.packageManager
        val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledApplications(0)
        }
        val filtered = packages
            .filter { info ->
                val system = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                includeSystem || !system
            }
            .sortedBy { info ->
                try {
                    pm.getApplicationLabel(info).toString().lowercase()
                } catch (_: Exception) {
                    info.packageName
                }
            }
        val canRead = PrivilegeCapability.MANAGE_APP_OPS in privilegedExecutor.currentStatus().capabilities
        val hyperOs = isHyperOs()
        if (!canRead) {
            return@withContext filtered.map { info ->
                toApp(pm, info, autostart = null, battery = null)
            }
        }
        val enriched = ArrayList<BackgroundControlsApp>(filtered.size)
        for (chunk in filtered.chunked(ENRICH_PARALLELISM)) {
            coroutineScope {
                val part = chunk.map { info ->
                    async {
                        val autostart = if (hyperOs) readAutostart(info.packageName) else null
                        val battery = readBattery(info.packageName)
                        toApp(pm, info, autostart, battery)
                    }
                }.awaitAll()
                enriched.addAll(part)
            }
        }
        enriched
    }

    suspend fun setAutostart(packageName: String, allowed: Boolean): Result<Unit> = withContext(ioDispatcher) {
        if (!canMutate()) return@withContext Result.failure("capability_missing:MANAGE_APP_OPS")
        if (!isHyperOs()) return@withContext Result.failure("not_hyperos")
        val mode = if (allowed) "allow" else "ignore"
        var lastFailure: Result.Failure? = null
        for (op in BackgroundControlsOps.AUTOSTART_OPS) {
            when (val r = privilegedExecutor.setAppOp(packageName, op, mode)) {
                is Result.Failure -> lastFailure = r
                is Result.Success -> Unit
            }
        }
        val success = lastFailure == null
        activityLog.record(
            ActivityLogEvents.create(
                type = ActivityLogType.PERMISSION,
                action = if (allowed) "autostart_allow" else "autostart_deny",
                success = success,
                packageName = packageName,
                detailKey = if (success) null else lastFailure?.message,
            ),
        )
        lastFailure ?: Result.success(Unit)
    }

    suspend fun setBatteryMode(packageName: String, mode: BatteryUsageMode): Result<Unit> = withContext(ioDispatcher) {
        if (!canMutate()) return@withContext Result.failure("capability_missing:MANAGE_APP_OPS")
        val (idleAction, runAny) = BatteryUsageModeMapper.toShell(mode)
        val idle = privilegedExecutor.deviceIdleWhitelist(idleAction, packageName)
        if (idle is Result.Failure) {
            activityLog.record(
                ActivityLogEvents.create(
                    type = ActivityLogType.PERMISSION,
                    action = "battery_$mode",
                    success = false,
                    packageName = packageName,
                    detailKey = idle.message,
                ),
            )
            return@withContext idle
        }
        val op = privilegedExecutor.setAppOp(packageName, BackgroundControlsOps.RUN_ANY_IN_BACKGROUND, runAny)
        val success = op is Result.Success
        activityLog.record(
            ActivityLogEvents.create(
                type = ActivityLogType.PERMISSION,
                action = "battery_$mode",
                success = success,
                packageName = packageName,
                detailKey = (op as? Result.Failure)?.message,
            ),
        )
        op
    }

    private fun toApp(pm: PackageManager, info: ApplicationInfo, autostart: Boolean?, battery: BatteryUsageMode?): BackgroundControlsApp {
        val label = try {
            pm.getApplicationLabel(info).toString()
        } catch (_: Exception) {
            info.packageName
        }
        return BackgroundControlsApp(
            packageName = info.packageName,
            label = label,
            isSystem = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
            autostartAllowed = autostart,
            batteryMode = battery,
        )
    }

    private suspend fun readAutostart(packageName: String): Boolean? =
        when (val out = privilegedExecutor.getAppOp(packageName, BackgroundControlsOps.AUTO_START)) {
            is Result.Failure -> {
                Timber.d("Autostart read failed for %s: %s", packageName, out.message)
                null
            }

            is Result.Success -> AppOpOutputParser.isAllowed(out.value)
        }

    private suspend fun readBattery(packageName: String): BatteryUsageMode? {
        val whitelist = when (val out = privilegedExecutor.deviceIdleWhitelist("=", packageName)) {
            is Result.Failure -> {
                Timber.d("deviceidle query failed for %s: %s", packageName, out.message)
                return null
            }

            is Result.Success -> DeviceIdleWhitelistParser.isWhitelisted(packageName, out.value)
        }
        val runAny = when (val out = privilegedExecutor.getAppOp(packageName, BackgroundControlsOps.RUN_ANY_IN_BACKGROUND)) {
            is Result.Failure -> "allow"
            is Result.Success -> AppOpOutputParser.parseMode(out.value) ?: "allow"
        }
        return BatteryUsageModeMapper.fromShell(whitelist, runAny)
    }

    companion object {
        private const val ENRICH_PARALLELISM = 6
    }
}

package dev.involvex.kiln.feature.permissions.freeze

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
import dev.involvex.kiln.core.model.FreezeApp
import dev.involvex.kiln.core.model.FreezeGate
import dev.involvex.kiln.core.model.PrivilegedExecutor
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

@Singleton
class FreezeRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val privilegedExecutor: PrivilegedExecutor,
    private val activityLog: ActivityLogRecorder,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    fun canMutate(): Boolean = FreezeGate.canMutate(privilegedExecutor.currentStatus().capabilities)

    suspend fun loadApps(includeSystem: Boolean): List<FreezeApp> = withContext(ioDispatcher) {
        val pm = context.packageManager
        val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledApplications(0)
        }
        packages
            .asSequence()
            .filter { info ->
                val system = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                includeSystem || !system
            }
            .map { info -> toApp(pm, info) }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    suspend fun setFrozen(packageName: String, frozen: Boolean, targetUserId: Int = 0): Result<Unit> = withContext(ioDispatcher) {
        if (!canMutate()) return@withContext Result.failure("capability_missing:DEBLOAT")
        val result = if (frozen) {
            privilegedExecutor.suspendPackage(packageName, targetUserId = targetUserId)
        } else {
            privilegedExecutor.unsuspendPackage(packageName, targetUserId = targetUserId)
        }
        val success = result is Result.Success
        val failureDetail = (result as? Result.Failure)?.message
        activityLog.record(
            ActivityLogEvents.create(
                type = ActivityLogType.DEBLOAT,
                action = if (frozen) "freeze_suspend" else "freeze_unsuspend",
                success = success,
                packageName = packageName,
                detailKey = buildString {
                    if (targetUserId != 0) append("user:$targetUserId")
                    if (failureDetail != null) {
                        if (isNotEmpty()) append(';')
                        append(failureDetail)
                    }
                }.ifEmpty { null },
            ),
        )
        result
    }

    private fun toApp(pm: PackageManager, info: ApplicationInfo): FreezeApp {
        val label = try {
            pm.getApplicationLabel(info).toString()
        } catch (_: Exception) {
            info.packageName
        }
        // FLAG_SUSPENDED since API 24; minSdk 26.
        val frozen = (info.flags and ApplicationInfo.FLAG_SUSPENDED) != 0
        return FreezeApp(
            packageName = info.packageName,
            label = label,
            isSystem = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
            isFrozen = frozen,
        )
    }
}

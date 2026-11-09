package dev.involvex.kiln.feature.permissions.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.involvex.kiln.core.common.IoDispatcher
import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.core.model.ActivityLogEvents
import dev.involvex.kiln.core.model.ActivityLogRecorder
import dev.involvex.kiln.core.model.ActivityLogType
import dev.involvex.kiln.core.model.AppPermissionEntry
import dev.involvex.kiln.core.model.PermissionAuditApp
import dev.involvex.kiln.core.model.PermissionAuditFilter
import dev.involvex.kiln.core.model.PermissionAuditGate
import dev.involvex.kiln.core.model.PrivilegeCapability
import dev.involvex.kiln.core.model.PrivilegedExecutor
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

data class PermissionMutationTarget(val packageName: String, val permission: String)

data class PermissionMutationResult(val packageName: String, val target: String, val success: Boolean, val detailKey: String?)

@Singleton
class PermissionAuditRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val privilegedExecutor: PrivilegedExecutor,
    private val activityLog: ActivityLogRecorder,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    suspend fun loadApps(): List<PermissionAuditApp> = withContext(ioDispatcher) {
        val pm = context.packageManager

        @Suppress("DEPRECATION")
        val flags = PackageManager.GET_PERMISSIONS
        val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // API 33+: typed flags; GET_PERMISSIONS still needed for requestedPermissions.
            pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(flags.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledPackages(flags)
        }
        val apps = packages.mapNotNull { info ->
            val dangerousGranted = permissionsOf(pm, info)
                .filter { it.isDangerous && it.granted }
            if (dangerousGranted.isEmpty()) return@mapNotNull null
            val appInfo = info.applicationInfo ?: return@mapNotNull null
            val label = try {
                pm.getApplicationLabel(appInfo).toString()
            } catch (_: Exception) {
                info.packageName
            }
            PermissionAuditApp(
                packageName = info.packageName,
                label = label,
                isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                dangerousGranted = dangerousGranted,
            )
        }
        PermissionAuditFilter.withDangerousGranted(apps).sortedBy { it.label.lowercase() }
    }

    suspend fun revoke(targets: List<PermissionMutationTarget>): List<PermissionMutationResult> = withContext(ioDispatcher) {
        if (!PermissionAuditGate.canMutate(privilegedExecutor.currentStatus().capabilities)) {
            return@withContext targets.map {
                PermissionMutationResult(
                    packageName = it.packageName,
                    target = it.permission,
                    success = false,
                    detailKey = "capability_missing:MANAGE_APP_OPS",
                )
            }
        }
        targets.map { target ->
            when (
                val result = privilegedExecutor.revokeRuntimePermission(
                    target.packageName,
                    target.permission,
                )
            ) {
                is Result.Success -> {
                    activityLog.record(
                        ActivityLogEvents.create(
                            type = ActivityLogType.PERMISSION,
                            action = "pm_revoke",
                            success = true,
                            packageName = target.packageName,
                            detailKey = target.permission.substringAfterLast('.'),
                        ),
                    )
                    PermissionMutationResult(target.packageName, target.permission, true, null)
                }

                is Result.Failure -> {
                    activityLog.record(
                        ActivityLogEvents.create(
                            type = ActivityLogType.PERMISSION,
                            action = "pm_revoke",
                            success = false,
                            packageName = target.packageName,
                            detailKey = result.message,
                        ),
                    )
                    PermissionMutationResult(
                        target.packageName,
                        target.permission,
                        false,
                        result.message,
                    )
                }
            }
        }
    }

    suspend fun setAppOp(packageName: String, op: String, mode: String): PermissionMutationResult = withContext(ioDispatcher) {
        if (PrivilegeCapability.MANAGE_APP_OPS !in privilegedExecutor.currentStatus().capabilities) {
            return@withContext PermissionMutationResult(
                packageName = packageName,
                target = "$op=$mode",
                success = false,
                detailKey = "capability_missing:MANAGE_APP_OPS",
            )
        }
        when (val result = privilegedExecutor.setAppOp(packageName, op, mode)) {
            is Result.Success -> {
                activityLog.record(
                    ActivityLogEvents.create(
                        type = ActivityLogType.PERMISSION,
                        action = "appops_set",
                        success = true,
                        packageName = packageName,
                        detailKey = "$op:$mode",
                    ),
                )
                PermissionMutationResult(packageName, "$op=$mode", true, null)
            }

            is Result.Failure -> {
                activityLog.record(
                    ActivityLogEvents.create(
                        type = ActivityLogType.PERMISSION,
                        action = "appops_set",
                        success = false,
                        packageName = packageName,
                        detailKey = result.message,
                    ),
                )
                PermissionMutationResult(packageName, "$op=$mode", false, result.message)
            }
        }
    }

    private fun permissionsOf(pm: PackageManager, info: PackageInfo): List<AppPermissionEntry> {
        val names = info.requestedPermissions ?: return emptyList()
        val flags = info.requestedPermissionsFlags
        return names.mapIndexed { index, name ->
            val granted = flags != null &&
                index < flags.size &&
                (flags[index] and PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0
            AppPermissionEntry(
                name = name,
                granted = granted,
                isDangerous = isDangerousPermission(pm, name),
            )
        }
    }

    private fun isDangerousPermission(pm: PackageManager, permission: String): Boolean = try {
        @Suppress("DEPRECATION")
        val permissionInfo = pm.getPermissionInfo(permission, 0)
        val level = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            permissionInfo.protection
        } else {
            @Suppress("DEPRECATION")
            permissionInfo.protectionLevel and PermissionInfo.PROTECTION_MASK_BASE
        }
        level == PermissionInfo.PROTECTION_DANGEROUS
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }
}

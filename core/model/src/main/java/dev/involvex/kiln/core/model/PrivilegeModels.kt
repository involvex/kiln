package dev.involvex.kiln.core.model

import dev.involvex.kiln.core.common.Result
import kotlinx.coroutines.flow.Flow

/** Active privilege tier. UI never asks “is Shizuku installed?” — it reads [PrivilegeStatus.capabilities]. */
enum class PrivilegeTier {
    NONE,
    SHIZUKU,
    ROOT,
}

/**
 * Capabilities exposed by the active [PrivilegedExecutor].
 * Features gate actions on these flags, not on package detection.
 */
enum class PrivilegeCapability {
    SILENT_INSTALL,
    SILENT_UNINSTALL,
    CLEAR_CACHE,
    RUN_SHELL_DIAGNOSTIC,
    ACCESS_ANDROID_DATA,
    DEBLOAT,
    FULL_BACKUP,
}

/** Shizuku setup assistant phases A→D (PLAN §4.9). */
enum class ShizukuSetupPhase {
    NOT_INSTALLED,
    NOT_RUNNING,
    NO_PERMISSION,
    READY,
}

/** Availability shown in the setup self-test matrix. */
enum class CapabilityAvailability {
    AVAILABLE,
    RESTRICTED,
    UNAVAILABLE,
}

data class SelfTestReport(
    val uid: Int,
    val selinuxContext: String?,
    val androidApi: Int,
    val shizukuApiVersion: Int?,
    val managerPackage: String?,
    val managerVersionName: String?,
    val rawOutput: String,
    val matrix: Map<PrivilegeCapability, CapabilityAvailability>,
)

data class PrivilegeStatus(
    val phase: ShizukuSetupPhase,
    val tier: PrivilegeTier,
    val capabilities: Set<PrivilegeCapability>,
    val managerPackage: String?,
    val managerLabel: String?,
    val selfTest: SelfTestReport?,
    val binderAlive: Boolean,
)

/**
 * Sole entry point for privileged work. Implementations live in `:core:privilege`.
 * Features must never call Shizuku / `su` directly.
 */
interface PrivilegedExecutor {
    fun observeStatus(): Flow<PrivilegeStatus>

    fun currentStatus(): PrivilegeStatus

    suspend fun refresh()

    /** Request Shizuku API permission (no-op for [PrivilegeTier.NONE]). */
    fun requestPermission(requestCode: Int)

    suspend fun runSelfTest(): Result<SelfTestReport>

    /** Silent install via allowlisted `pm install -r` when [PrivilegeCapability.SILENT_INSTALL] is present. */
    suspend fun installApk(apkAbsolutePath: String): Result<Unit>

    /** Silent uninstall when [PrivilegeCapability.SILENT_UNINSTALL] is present. */
    suspend fun uninstall(packageName: String, keepData: Boolean): Result<Unit>

    /**
     * Clear cache for [packageName], or trim all caches when null.
     * Requires [PrivilegeCapability.CLEAR_CACHE].
     */
    suspend fun clearCache(packageName: String?): Result<Long>
}

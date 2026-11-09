package dev.involvex.kiln.core.privilege

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.RemoteException
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.involvex.kiln.core.common.IoDispatcher
import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.core.model.ActivityLogEvents
import dev.involvex.kiln.core.model.ActivityLogRecorder
import dev.involvex.kiln.core.model.ActivityLogType
import dev.involvex.kiln.core.model.AndroidSharedRoot
import dev.involvex.kiln.core.model.PrivilegeCapability
import dev.involvex.kiln.core.model.PrivilegeStatus
import dev.involvex.kiln.core.model.PrivilegeTier
import dev.involvex.kiln.core.model.PrivilegedExecutor
import dev.involvex.kiln.core.model.SelfTestReport
import dev.involvex.kiln.core.model.ShizukuSetupPhase
import dev.involvex.kiln.core.privilege.shell.ShellAllowlist
import dev.involvex.kiln.core.privilege.shizuku.KilnUserService
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import timber.log.Timber

/**
 * Active privilege coordinator: Shizuku when ready, otherwise Tier NONE.
 * ShizukuPlus is detected for setup UX; API differences deferred (same Shizuku binder API).
 * RootExecutor deferred (opt-in + libsu) — not activated in M4.
 */
@Singleton
@Suppress("TooManyFunctions") // Mirrors PrivilegedExecutor surface across milestones.
class DefaultPrivilegedExecutor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val managerDetector: ShizukuManagerDetector,
    private val activityLog: ActivityLogRecorder,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : PrivilegedExecutor {
    private val statusFlow = MutableStateFlow(buildStatus(selfTest = null))
    private val userServiceMutex = Mutex()
    private var userService: IKilnUserService? = null
    private var userServiceConnection: ServiceConnection? = null

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        Timber.d("Shizuku binder received")
        publishStatus()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        Timber.d("Shizuku binder dead")
        userService = null
        publishStatus()
    }

    private val permissionListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
        Timber.d("Shizuku permission result=%s", grantResult)
        publishStatus()
    }

    init {
        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(permissionListener)
        publishStatus()
    }

    override fun observeStatus(): Flow<PrivilegeStatus> = statusFlow.asStateFlow()

    override fun currentStatus(): PrivilegeStatus = statusFlow.value

    override suspend fun refresh() {
        publishStatus()
    }

    override fun requestPermission(requestCode: Int) {
        if (!safePing()) return
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            publishStatus()
            return
        }
        try {
            Shizuku.requestPermission(requestCode)
        } catch (t: Throwable) {
            Timber.w(t, "Shizuku.requestPermission failed")
        }
    }

    override suspend fun runSelfTest(): Result<SelfTestReport> = withContext(ioDispatcher) {
        if (!hasPermission()) {
            return@withContext Result.failure("permission_required")
        }
        when (val out = invokeAllowlisted(ShellAllowlist.ID, emptyList())) {
            is Result.Failure -> out

            is Result.Success -> {
                val parsed = parseIdOutput(out.value)
                val manager = managerDetector.detect()
                val report = SelfTestReport(
                    uid = parsed.uid,
                    selinuxContext = parsed.context,
                    androidApi = Build.VERSION.SDK_INT,
                    shizukuApiVersion = safeShizukuVersion(),
                    managerPackage = manager?.packageName,
                    managerVersionName = manager?.versionName,
                    rawOutput = out.value,
                    matrix = CapabilityMatrix.fromUid(parsed.uid, PrivilegeTier.SHIZUKU),
                )
                publishStatus(report)
                Result.success(report)
            }
        }
    }

    override suspend fun installApk(apkAbsolutePath: String, allowDowngrade: Boolean): Result<Unit> = withContext(ioDispatcher) {
        if (PrivilegeCapability.SILENT_INSTALL !in currentStatus().capabilities) {
            return@withContext Result.failure("capability_missing:SILENT_INSTALL")
        }
        if (!ShellAllowlist.isSafeApkPath(apkAbsolutePath)) {
            return@withContext Result.failure("invalid_apk_path")
        }
        val commandId = if (allowDowngrade) {
            ShellAllowlist.PM_INSTALL_DOWNGRADE
        } else {
            ShellAllowlist.PM_INSTALL
        }
        when (val out = invokeAllowlisted(commandId, listOf(apkAbsolutePath))) {
            is Result.Failure -> out

            is Result.Success -> {
                if (out.value.startsWith("ERR:")) {
                    Result.failure(out.value)
                } else {
                    Result.success(Unit)
                }
            }
        }
    }

    override suspend fun uninstall(packageName: String, keepData: Boolean): Result<Unit> = withContext(ioDispatcher) {
        if (PrivilegeCapability.SILENT_UNINSTALL !in currentStatus().capabilities) {
            return@withContext Result.failure("capability_missing:SILENT_UNINSTALL")
        }
        if (!ShellAllowlist.isSafePackageName(packageName)) {
            return@withContext Result.failure("invalid_package")
        }
        val args = if (keepData) listOf(packageName, "keep") else listOf(packageName)
        when (val out = invokeAllowlisted(ShellAllowlist.PM_UNINSTALL, args)) {
            is Result.Failure -> out

            is Result.Success -> {
                if (out.value.startsWith("ERR:")) {
                    Result.failure(out.value)
                } else {
                    Result.success(Unit)
                }
            }
        }
    }

    override suspend fun clearCache(packageName: String?): Result<Long> = withContext(ioDispatcher) {
        if (PrivilegeCapability.CLEAR_CACHE !in currentStatus().capabilities) {
            return@withContext Result.failure("capability_missing:CLEAR_CACHE")
        }
        val commandId: String
        val args: List<String>
        if (packageName == null) {
            // Large free-space target so the system trims as much cache as it will allow.
            commandId = ShellAllowlist.PM_TRIM_CACHES
            args = listOf(TRIM_ALL_DESIRED_BYTES)
        } else {
            if (!ShellAllowlist.isSafePackageName(packageName)) {
                return@withContext Result.failure("invalid_package")
            }
            commandId = ShellAllowlist.PM_CLEAR_CACHE
            args = listOf(packageName)
        }
        when (val out = invokeAllowlisted(commandId, args)) {
            is Result.Failure -> out

            is Result.Success -> {
                if (out.value.startsWith("ERR:")) {
                    Result.failure(out.value)
                } else {
                    // pm does not report freed bytes reliably; 0 means “command succeeded”.
                    Result.success(0L)
                }
            }
        }
    }

    override suspend fun disableUserPackage(packageName: String): Result<Unit> =
        runDebloatCommand(ShellAllowlist.PM_DISABLE_USER, packageName)

    override suspend fun enablePackage(packageName: String): Result<Unit> = runDebloatCommand(ShellAllowlist.PM_ENABLE, packageName)

    override suspend fun installExistingPackage(packageName: String): Result<Unit> =
        runDebloatCommand(ShellAllowlist.PM_INSTALL_EXISTING, packageName)

    override suspend fun revokeRuntimePermission(packageName: String, permission: String): Result<Unit> = withContext(ioDispatcher) {
        if (PrivilegeCapability.MANAGE_APP_OPS !in currentStatus().capabilities) {
            return@withContext Result.failure("capability_missing:MANAGE_APP_OPS")
        }
        if (!ShellAllowlist.isSafePackageName(packageName)) {
            return@withContext Result.failure("invalid_package")
        }
        if (!ShellAllowlist.isSafePermissionName(permission)) {
            return@withContext Result.failure("invalid_permission")
        }
        when (val out = invokeAllowlisted(ShellAllowlist.PM_REVOKE, listOf(packageName, permission))) {
            is Result.Failure -> out

            is Result.Success -> {
                if (out.value.startsWith("ERR:")) {
                    Result.failure(out.value)
                } else {
                    Result.success(Unit)
                }
            }
        }
    }

    override suspend fun setAppOp(packageName: String, op: String, mode: String): Result<Unit> = withContext(ioDispatcher) {
        if (PrivilegeCapability.MANAGE_APP_OPS !in currentStatus().capabilities) {
            return@withContext Result.failure("capability_missing:MANAGE_APP_OPS")
        }
        if (!ShellAllowlist.isSafePackageName(packageName)) {
            return@withContext Result.failure("invalid_package")
        }
        if (!ShellAllowlist.isAllowedAppOp(op) || !ShellAllowlist.isAllowedAppOpMode(mode)) {
            return@withContext Result.failure("invalid_appop")
        }
        when (val out = invokeAllowlisted(ShellAllowlist.APPOPS_SET, listOf(packageName, op, mode))) {
            is Result.Failure -> out

            is Result.Success -> {
                if (out.value.startsWith("ERR:")) {
                    Result.failure(out.value)
                } else {
                    Result.success(Unit)
                }
            }
        }
    }

    override suspend fun listAndroidSharedPackageDirs(root: AndroidSharedRoot): Result<List<String>> = withContext(ioDispatcher) {
        if (PrivilegeCapability.ACCESS_ANDROID_DATA !in currentStatus().capabilities) {
            return@withContext Result.failure("capability_missing:ACCESS_ANDROID_DATA")
        }
        when (val out = invokeAllowlisted(ShellAllowlist.LS_ANDROID_ROOT, listOf(root.pathKey))) {
            is Result.Failure -> out

            is Result.Success -> {
                if (out.value.startsWith("ERR:")) {
                    Result.failure(out.value)
                } else {
                    val names = out.value.lineSequence()
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .toList()
                    Result.success(names)
                }
            }
        }
    }

    override suspend fun measureAndroidSharedPackageDir(root: AndroidSharedRoot, packageName: String): Result<Long> =
        withContext(ioDispatcher) {
            if (PrivilegeCapability.ACCESS_ANDROID_DATA !in currentStatus().capabilities) {
                return@withContext Result.failure("capability_missing:ACCESS_ANDROID_DATA")
            }
            if (!ShellAllowlist.isSafePackageName(packageName)) {
                return@withContext Result.failure("invalid_package")
            }
            when (val out = invokeAllowlisted(ShellAllowlist.DU_ANDROID_PKG, listOf(root.pathKey, packageName))) {
                is Result.Failure -> out

                is Result.Success -> {
                    if (out.value.startsWith("ERR:")) {
                        Result.failure(out.value)
                    } else {
                        // `du -s` prints "<kib>\t<path>" (or spaces); treat first token as KiB.
                        val token = out.value.trim().split(Regex("\\s+")).firstOrNull()
                        val kib = token?.toLongOrNull()
                        if (kib == null) {
                            Result.failure("du_parse")
                        } else {
                            Result.success(kib * 1024L)
                        }
                    }
                }
            }
        }

    override suspend fun deleteAndroidSharedPackageDir(root: AndroidSharedRoot, packageName: String): Result<Unit> =
        withContext(ioDispatcher) {
            if (PrivilegeCapability.ACCESS_ANDROID_DATA !in currentStatus().capabilities) {
                return@withContext Result.failure("capability_missing:ACCESS_ANDROID_DATA")
            }
            if (!ShellAllowlist.isSafePackageName(packageName)) {
                return@withContext Result.failure("invalid_package")
            }
            when (val out = invokeAllowlisted(ShellAllowlist.RM_ANDROID_PKG, listOf(root.pathKey, packageName))) {
                is Result.Failure -> out

                is Result.Success -> {
                    if (out.value.startsWith("ERR:")) {
                        Result.failure(out.value)
                    } else {
                        Result.success(Unit)
                    }
                }
            }
        }

    private suspend fun runDebloatCommand(commandId: String, packageName: String): Result<Unit> = withContext(ioDispatcher) {
        if (PrivilegeCapability.DEBLOAT !in currentStatus().capabilities) {
            return@withContext Result.failure("capability_missing:DEBLOAT")
        }
        if (!ShellAllowlist.isSafePackageName(packageName)) {
            return@withContext Result.failure("invalid_package")
        }
        when (val out = invokeAllowlisted(commandId, listOf(packageName))) {
            is Result.Failure -> out

            is Result.Success -> {
                if (out.value.startsWith("ERR:")) {
                    Result.failure(out.value)
                } else {
                    Result.success(Unit)
                }
            }
        }
    }

    private suspend fun invokeAllowlisted(commandId: String, args: List<String>): Result<String> {
        val service = ensureUserService()
            ?: return Result.failure("user_service_unavailable").also {
                recordShell(commandId, args, success = false, detailKey = "user_service_unavailable")
            }
        return try {
            val raw = service.runAllowlisted(commandId, args.toTypedArray())
            val value = raw ?: "ERR:null"
            val ok = !value.startsWith("ERR:")
            recordShell(
                commandId = commandId,
                args = args,
                success = ok,
                detailKey = if (ok) null else "shell_error",
            )
            Result.success(value)
        } catch (e: RemoteException) {
            Timber.w(e, "UserService remote failure")
            userService = null
            recordShell(commandId, args, success = false, detailKey = "remote")
            Result.failure("remote:${e.message}", e)
        }
    }

    private suspend fun recordShell(commandId: String, args: List<String>, success: Boolean, detailKey: String?) {
        // Never log APK paths or absolute FS paths; only package-shaped args.
        val packageName = when (commandId) {
            ShellAllowlist.PM_INSTALL,
            ShellAllowlist.PM_INSTALL_DOWNGRADE,
            ShellAllowlist.PM_TRIM_CACHES,
            ShellAllowlist.ID,
            ShellAllowlist.LS_ANDROID_ROOT,
            -> null

            ShellAllowlist.DU_ANDROID_PKG,
            ShellAllowlist.RM_ANDROID_PKG,
            -> args.getOrNull(1)?.takeIf { ShellAllowlist.isSafePackageName(it) }

            else -> args.firstOrNull()?.takeIf { ShellAllowlist.isSafePackageName(it) }
        }
        activityLog.record(
            ActivityLogEvents.create(
                type = ActivityLogType.SHELL,
                action = commandId,
                success = success,
                packageName = packageName,
                detailKey = detailKey,
            ),
        )
    }

    private suspend fun ensureUserService(): IKilnUserService? = userServiceMutex.withLock {
        userService?.let { if (it.asBinder().pingBinder()) return@withLock it }
        if (!hasPermission()) return@withLock null
        if (Shizuku.getVersion() < 10) {
            Timber.w("Shizuku API %d < 10; UserService unsupported", Shizuku.getVersion())
            return@withLock null
        }
        val args = Shizuku.UserServiceArgs(
            ComponentName(context.packageName, KilnUserService::class.java.name),
        )
            .daemon(false)
            .processNameSuffix("privilege")
            .debuggable((context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0)
            .version(versionCode())

        suspendCancellableCoroutine { cont ->
            val connection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                    if (binder != null && binder.pingBinder()) {
                        val svc = IKilnUserService.Stub.asInterface(binder)
                        userService = svc
                        if (cont.isActive) cont.resume(svc)
                    } else if (cont.isActive) {
                        cont.resume(null)
                    }
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    userService = null
                }
            }
            userServiceConnection = connection
            cont.invokeOnCancellation {
                try {
                    Shizuku.unbindUserService(args, connection, true)
                } catch (_: Throwable) {
                }
            }
            try {
                Shizuku.bindUserService(args, connection)
            } catch (t: Throwable) {
                Timber.w(t, "bindUserService failed")
                if (cont.isActive) cont.resume(null)
            }
        }
    }

    private fun publishStatus(selfTest: SelfTestReport? = statusFlow.value.selfTest) {
        statusFlow.value = buildStatus(selfTest)
    }

    private fun buildStatus(selfTest: SelfTestReport?): PrivilegeStatus {
        val manager = managerDetector.detect()
        val binderAlive = safePing()
        val permissionGranted = binderAlive &&
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        val phase = when {
            manager == null -> ShizukuSetupPhase.NOT_INSTALLED
            !binderAlive -> ShizukuSetupPhase.NOT_RUNNING
            !permissionGranted -> ShizukuSetupPhase.NO_PERMISSION
            else -> ShizukuSetupPhase.READY
        }
        val tier = if (phase == ShizukuSetupPhase.READY) PrivilegeTier.SHIZUKU else PrivilegeTier.NONE
        // Optimistic shell capabilities until self-test reports the real UID (usually 2000).
        val capabilities = when {
            phase != ShizukuSetupPhase.READY -> emptySet()
            selfTest != null -> CapabilityMatrix.capabilities(selfTest.uid, PrivilegeTier.SHIZUKU)
            else -> CapabilityMatrix.capabilities(CapabilityMatrix.SHELL_UID, PrivilegeTier.SHIZUKU)
        }
        return PrivilegeStatus(
            phase = phase,
            tier = tier,
            capabilities = capabilities,
            managerPackage = manager?.packageName,
            managerLabel = manager?.label,
            selfTest = selfTest,
            binderAlive = binderAlive,
        )
    }

    private fun hasPermission(): Boolean = safePing() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED

    private fun safePing(): Boolean = try {
        Shizuku.pingBinder()
    } catch (_: Throwable) {
        false
    }

    private fun safeShizukuVersion(): Int? = try {
        if (safePing()) Shizuku.getVersion() else null
    } catch (_: Throwable) {
        null
    }

    private fun versionCode(): Int {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // PackageInfo.getLongVersionCode: API 28+
            info.longVersionCode.toInt()
        } else {
            @Suppress("DEPRECATION")
            info.versionCode
        }
    }

    private data class IdParse(val uid: Int, val context: String?)

    private fun parseIdOutput(raw: String): IdParse {
        // Typical: uid=2000(shell) gid=2000(shell) … context=u:r:shell:s0
        val uid = Regex("""uid=(\d+)""").find(raw)?.groupValues?.get(1)?.toIntOrNull() ?: -1
        val ctx = Regex("""context=([^\s]+)""").find(raw)?.groupValues?.get(1)
        return IdParse(uid, ctx)
    }

    companion object {
        /** ~931 GiB — high enough that trim-caches clears as much as the ROM allows. */
        private const val TRIM_ALL_DESIRED_BYTES = "999999999999"
    }
}

package dev.involvex.kiln.feature.termux.run

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.involvex.kiln.core.model.ActivityLogEvents
import dev.involvex.kiln.core.model.ActivityLogRecorder
import dev.involvex.kiln.core.model.ActivityLogType
import dev.involvex.kiln.feature.termux.TermuxPackageDetector
import dev.involvex.kiln.feature.termux.TermuxResultReceiver
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

sealed interface TermuxLaunchOutcome {
    data class Started(val executionId: Int) : TermuxLaunchOutcome

    data object PackageNotInstalled : TermuxLaunchOutcome

    data object PermissionDenied : TermuxLaunchOutcome

    data class StartFailed(val message: String) : TermuxLaunchOutcome
}

@Singleton
class TermuxCommandLauncher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val detector: TermuxPackageDetector,
    private val activityLog: ActivityLogRecorder,
) {
    private val nextExecutionId = AtomicInteger(1_000)
    private val logScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun nextExecutionId(): Int = nextExecutionId.getAndIncrement()

    fun isPackageInstalled(packageId: String): Boolean = try {
        context.packageManager.getPackageInfo(packageId, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    fun hasRunCommandPermission(packageId: String): Boolean {
        val permission = TermuxRunCommandApi.permission(packageId)
        return ContextCompat.checkSelfPermission(context, permission) ==
            PackageManager.PERMISSION_GRANTED
    }

    fun launch(request: TermuxRunCommandRequest): TermuxLaunchOutcome {
        if (!isPackageInstalled(request.packageId)) {
            recordTermux("run_command_failed", success = false, packageId = request.packageId, detailKey = "package_not_installed")
            return TermuxLaunchOutcome.PackageNotInstalled
        }
        if (!hasRunCommandPermission(request.packageId)) {
            recordTermux("run_command_failed", success = false, packageId = request.packageId, detailKey = "permission_denied")
            return TermuxLaunchOutcome.PermissionDenied
        }

        val resultIntent = Intent(context, TermuxResultReceiver::class.java).apply {
            putExtra(TermuxRunCommandApi.EXTRA_EXECUTION_ID, request.executionId)
        }
        val pendingFlags = PendingIntent.FLAG_ONE_SHOT or
            // API 31+: PendingIntent must be mutable so Termux can attach the result bundle.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE
            } else {
                0
            }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            request.executionId,
            resultIntent,
            pendingFlags,
        )
        val serviceComponent = detector.resolveServiceComponent(request.packageId)
        val serviceIntent = TermuxRunCommandIntentBuilder.build(request, pendingIntent, serviceComponent)
        return try {
            context.startService(serviceIntent)
            recordTermux(
                action = "run_command_started",
                success = true,
                packageId = request.packageId,
                refId = request.executionId.toString(),
            )
            TermuxLaunchOutcome.Started(request.executionId)
        } catch (e: Exception) {
            Timber.e(e, "Failed to start Termux RunCommandService")
            recordTermux(
                action = "run_command_failed",
                success = false,
                packageId = request.packageId,
                detailKey = "start_failed",
            )
            TermuxLaunchOutcome.StartFailed(e.message ?: e.javaClass.simpleName)
        }
    }

    private fun recordTermux(action: String, success: Boolean, packageId: String, detailKey: String? = null, refId: String? = null) {
        logScope.launch {
            activityLog.record(
                ActivityLogEvents.create(
                    type = ActivityLogType.TERMUX,
                    action = action,
                    success = success,
                    packageName = packageId,
                    detailKey = detailKey,
                    refId = refId,
                ),
            )
        }
    }
}

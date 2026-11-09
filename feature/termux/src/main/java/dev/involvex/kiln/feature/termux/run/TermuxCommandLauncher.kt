package dev.involvex.kiln.feature.termux.run

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.involvex.kiln.feature.termux.TermuxPackageDetector
import dev.involvex.kiln.feature.termux.TermuxResultReceiver
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
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
) {
    private val nextExecutionId = AtomicInteger(1_000)

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
            return TermuxLaunchOutcome.PackageNotInstalled
        }
        if (!hasRunCommandPermission(request.packageId)) {
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
            TermuxLaunchOutcome.Started(request.executionId)
        } catch (e: Exception) {
            Timber.e(e, "Failed to start Termux RunCommandService")
            TermuxLaunchOutcome.StartFailed(e.message ?: e.javaClass.simpleName)
        }
    }
}

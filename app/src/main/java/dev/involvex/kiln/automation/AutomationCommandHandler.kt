package dev.involvex.kiln.automation

import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.involvex.kiln.core.common.Result as KilnResult
import dev.involvex.kiln.core.model.AppUpdateInfo
import dev.involvex.kiln.core.model.AppUpdateRepository
import dev.involvex.kiln.core.model.AutomationApi
import dev.involvex.kiln.core.model.AutomationPreferences
import dev.involvex.kiln.core.model.BackgroundUpdateScheduler
import dev.involvex.kiln.core.model.InstallQueue
import dev.involvex.kiln.core.model.MatchConfidence
import dev.involvex.kiln.core.model.PrivilegedExecutor
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import timber.log.Timber

data class AutomationOutcome(val ok: Boolean, val message: String)

@Singleton
class AutomationCommandHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val automationPreferences: AutomationPreferences,
    private val scheduler: BackgroundUpdateScheduler,
    private val appUpdateRepository: AppUpdateRepository,
    private val installQueue: InstallQueue,
    private val privilegedExecutor: PrivilegedExecutor,
    private val failureNotifier: AutomationFailureNotifier,
) {
    suspend fun handle(intent: Intent): AutomationOutcome {
        if (!automationPreferences.current().enabled) {
            return failAndBroadcast(intent.action, "automation_disabled")
        }
        val command = AutomationCommandParser.parse(intent).getOrElse { error ->
            return failAndBroadcast(intent.action, error.message ?: "invalid_intent")
        }
        val outcome = when (command) {
            AutomationCommand.CheckUpdates -> {
                scheduler.enqueueImmediate()
                AutomationOutcome(ok = true, message = "check_queued")
            }

            is AutomationCommand.InstallUpdate -> installManagedUpdate(command.packageName)

            is AutomationCommand.InstallFromUrl -> installFromUrl(command)

            is AutomationCommand.CleanCache -> cleanCache(command.packageName)
        }
        broadcastResult(intent.action, outcome)
        if (!outcome.ok) {
            failureNotifier.notifyFailure(intent.action, outcome.message)
        }
        return outcome
    }

    private suspend fun installManagedUpdate(packageName: String): AutomationOutcome {
        appUpdateRepository.refreshUpdates()
        val update = appUpdateRepository.updates().first()[packageName]
            ?: return AutomationOutcome(ok = false, message = "no_update")
        if (update.watchOnly) {
            return AutomationOutcome(ok = false, message = "watch_only")
        }
        if (!update.signatureMatches) {
            return AutomationOutcome(ok = false, message = "signature_mismatch")
        }
        return when (val result = installQueue.enqueue(update)) {
            is KilnResult.Success -> AutomationOutcome(ok = true, message = "install_queued:${result.value}")
            is KilnResult.Failure -> AutomationOutcome(ok = false, message = result.message)
        }
    }

    private suspend fun installFromUrl(command: AutomationCommand.InstallFromUrl): AutomationOutcome {
        val update = AppUpdateInfo(
            packageName = command.packageName,
            availableVersionName = command.versionName,
            availableVersionCode = command.versionCode,
            sourceId = "automation",
            sourceLabel = "Automation",
            confidence = MatchConfidence.MEDIUM,
            signatureMatches = false,
            downloadUrl = command.url,
            apkSha256 = command.sha256,
        )
        return when (val result = installQueue.enqueue(update)) {
            is KilnResult.Success -> AutomationOutcome(ok = true, message = "install_queued:${result.value}")
            is KilnResult.Failure -> AutomationOutcome(ok = false, message = result.message)
        }
    }

    private suspend fun cleanCache(packageName: String?): AutomationOutcome =
        when (val result = privilegedExecutor.clearCache(packageName)) {
            is KilnResult.Success -> AutomationOutcome(ok = true, message = "cache_cleared:${result.value}")
            is KilnResult.Failure -> AutomationOutcome(ok = false, message = result.message)
        }

    private fun failAndBroadcast(requestAction: String?, message: String): AutomationOutcome {
        val outcome = AutomationOutcome(ok = false, message = message)
        broadcastResult(requestAction, outcome)
        failureNotifier.notifyFailure(requestAction, message)
        return outcome
    }

    private fun broadcastResult(requestAction: String?, outcome: AutomationOutcome) {
        // No setPackage: same-signer callers holding AUTOMATION can register for ACTION_RESULT.
        val result = Intent(AutomationApi.ACTION_RESULT).apply {
            putExtra(AutomationApi.EXTRA_REQUEST_ACTION, requestAction)
            putExtra(AutomationApi.EXTRA_OK, outcome.ok)
            putExtra(AutomationApi.EXTRA_MESSAGE, outcome.message)
        }
        runCatching {
            context.sendBroadcast(result, AutomationApi.PERMISSION)
        }.onFailure { error ->
            Timber.w(error, "Failed to send automation result broadcast")
        }
    }
}

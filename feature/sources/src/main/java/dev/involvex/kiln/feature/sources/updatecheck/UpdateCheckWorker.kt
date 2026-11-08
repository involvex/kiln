package dev.involvex.kiln.feature.sources.updatecheck

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.involvex.kiln.core.common.Result as KilnResult
import dev.involvex.kiln.core.model.AppUpdateRepository
import dev.involvex.kiln.core.model.BackgroundUpdatePreferences
import dev.involvex.kiln.core.model.InstallQueue
import kotlinx.coroutines.flow.first
import timber.log.Timber

@HiltWorker
class UpdateCheckWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val appUpdateRepository: AppUpdateRepository,
    private val preferences: BackgroundUpdatePreferences,
    private val installQueue: InstallQueue,
    private val notifier: UpdateCheckNotifier,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val prefs = preferences.current()
        if (!prefs.enabled && !inputData.getBoolean(KEY_FORCE, false)) {
            Timber.d("Update check skipped (disabled)")
            return Result.success()
        }
        return when (val check = appUpdateRepository.checkForUpdates()) {
            is KilnResult.Failure -> {
                Timber.w("Background update check failed: %s", check.message)
                Result.retry()
            }

            is KilnResult.Success -> {
                val updates = appUpdateRepository.updates().first()
                val count = check.value.updatesFound
                if (count > 0) {
                    notifier.notifyUpdatesFound(count)
                }
                val toInstall = AutoInstallPolicy.selectForEnqueue(
                    updates = updates.values,
                    autoInstallMatching = prefs.autoInstallMatching,
                )
                for (update in toInstall) {
                    when (val enqueued = installQueue.enqueue(update)) {
                        is KilnResult.Failure ->
                            Timber.w("Auto-enqueue %s failed: %s", update.packageName, enqueued.message)

                        is KilnResult.Success ->
                            Timber.d("Auto-enqueued %s", update.packageName)
                    }
                }
                Result.success()
            }
        }
    }

    companion object {
        const val WORK_NAME_PERIODIC = "kiln_update_check_periodic"
        const val WORK_NAME_ONESHOT = "kiln_update_check_oneshot"
        const val KEY_FORCE = "force"
    }
}

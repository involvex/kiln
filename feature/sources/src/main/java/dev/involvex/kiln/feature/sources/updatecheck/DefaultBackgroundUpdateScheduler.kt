package dev.involvex.kiln.feature.sources.updatecheck

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.involvex.kiln.core.model.BackgroundUpdatePrefs
import dev.involvex.kiln.core.model.BackgroundUpdateScheduler
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class DefaultBackgroundUpdateScheduler @Inject constructor(@ApplicationContext private val context: Context) : BackgroundUpdateScheduler {
    private val workManager: WorkManager get() = WorkManager.getInstance(context)

    override fun apply(prefs: BackgroundUpdatePrefs) {
        if (!prefs.enabled) {
            workManager.cancelUniqueWork(UpdateCheckWorker.WORK_NAME_PERIODIC)
            Timber.d("Cancelled periodic update check")
            return
        }
        val request = PeriodicWorkRequestBuilder<UpdateCheckWorker>(
            REPEAT_INTERVAL_HOURS,
            TimeUnit.HOURS,
        )
            .setConstraints(UpdateCheckConstraints.from(prefs))
            .addTag(UpdateCheckWorker.WORK_NAME_PERIODIC)
            .build()
        workManager.enqueueUniquePeriodicWork(
            UpdateCheckWorker.WORK_NAME_PERIODIC,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
        Timber.d("Scheduled periodic update check (unmeteredOnly=%s)", prefs.unmeteredOnly)
    }

    override fun enqueueImmediate() {
        // Check-now should run on cellular too so QA is not blocked by Wi‑Fi-only.
        val constraints = UpdateCheckConstraints.from(
            BackgroundUpdatePrefs(enabled = true, unmeteredOnly = false),
        )
        val request = OneTimeWorkRequestBuilder<UpdateCheckWorker>()
            .setInputData(workDataOf(UpdateCheckWorker.KEY_FORCE to true))
            .setConstraints(constraints)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .addTag(UpdateCheckWorker.WORK_NAME_ONESHOT)
            .build()
        workManager.enqueueUniqueWork(
            UpdateCheckWorker.WORK_NAME_ONESHOT,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    companion object {
        const val REPEAT_INTERVAL_HOURS = 12L
    }
}

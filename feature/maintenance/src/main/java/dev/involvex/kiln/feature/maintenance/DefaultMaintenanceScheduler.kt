package dev.involvex.kiln.feature.maintenance

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.involvex.kiln.core.model.MaintenanceDefaults
import dev.involvex.kiln.core.model.MaintenancePrefs
import dev.involvex.kiln.core.model.MaintenanceScheduler
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class DefaultMaintenanceScheduler @Inject constructor(@ApplicationContext private val context: Context) : MaintenanceScheduler {
    private val workManager: WorkManager get() = WorkManager.getInstance(context)

    override fun apply(prefs: MaintenancePrefs) {
        if (!prefs.enabled) {
            workManager.cancelUniqueWork(MaintenanceWorker.WORK_NAME_PERIODIC)
            Timber.d("Cancelled periodic maintenance")
            return
        }
        val request = PeriodicWorkRequestBuilder<MaintenanceWorker>(
            MaintenanceDefaults.INTERVAL_DAYS,
            TimeUnit.DAYS,
        )
            .setConstraints(MaintenanceConstraints.forPeriodic())
            .addTag(MaintenanceWorker.WORK_NAME_PERIODIC)
            .build()
        workManager.enqueueUniquePeriodicWork(
            MaintenanceWorker.WORK_NAME_PERIODIC,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
        Timber.d("Scheduled periodic maintenance (7d, unmetered+charging)")
    }

    override fun enqueueImmediate() {
        val request = OneTimeWorkRequestBuilder<MaintenanceWorker>()
            .setInputData(workDataOf(MaintenanceWorker.KEY_FORCE to true))
            .setConstraints(MaintenanceConstraints.forImmediate())
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .addTag(MaintenanceWorker.WORK_NAME_ONESHOT)
            .build()
        workManager.enqueueUniqueWork(
            MaintenanceWorker.WORK_NAME_ONESHOT,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }
}

package dev.involvex.kiln.feature.maintenance

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.involvex.kiln.core.model.MaintenanceDefaults
import dev.involvex.kiln.core.model.MaintenancePreferences
import dev.involvex.kiln.core.model.MaintenanceRunRecord
import dev.involvex.kiln.core.model.MaintenanceRunStore
import dev.involvex.kiln.core.model.MaintenanceTaskResult
import timber.log.Timber

@HiltWorker
class MaintenanceWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val preferences: MaintenancePreferences,
    private val runner: MaintenanceRunner,
    private val runStore: MaintenanceRunStore,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val force = inputData.getBoolean(KEY_FORCE, false)
        val prefs = preferences.current()
        if (!prefs.enabled && !force) {
            Timber.d("Maintenance skipped (disabled)")
            return Result.success()
        }
        if (!prefs.enabled && force) {
            val now = System.currentTimeMillis()
            runStore.append(
                MaintenanceRunRecord(
                    startedAtEpochMs = now,
                    finishedAtEpochMs = now,
                    ok = true,
                    tasks = listOf(
                        MaintenanceTaskResult(
                            taskId = "master",
                            status = MaintenanceDefaults.STATUS_SKIPPED,
                            detail = "maintenance_disabled",
                        ),
                    ),
                ),
            )
            Timber.d("Maintenance Run now while master off — recorded skip")
            return Result.success()
        }
        val record = runner.run(prefs)
        runStore.append(record)
        Timber.d("Maintenance finished ok=%s tasks=%d", record.ok, record.tasks.size)
        return Result.success()
    }

    companion object {
        const val WORK_NAME_PERIODIC = "kiln_maintenance_periodic"
        const val WORK_NAME_ONESHOT = "kiln_maintenance_oneshot"
        const val KEY_FORCE = "force"
    }
}

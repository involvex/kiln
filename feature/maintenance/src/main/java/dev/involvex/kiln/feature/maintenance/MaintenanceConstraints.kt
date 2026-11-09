package dev.involvex.kiln.feature.maintenance

import androidx.work.Constraints
import androidx.work.NetworkType

object MaintenanceConstraints {
    /** Weekly runs: Wi‑Fi (unmetered) and charging per PLAN §12. */
    fun forPeriodic(): Constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.UNMETERED)
        .setRequiresCharging(true)
        .build()

    /**
     * Run-now: no charging/battery constraints (WorkManager forbids them on expedited;
     * we also skip charging so the user can trigger immediately).
     */
    fun forImmediate(): Constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
        .build()
}

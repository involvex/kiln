package dev.involvex.kiln.feature.sources.updatecheck

import androidx.work.Constraints
import androidx.work.NetworkType
import dev.involvex.kiln.core.model.BackgroundUpdatePrefs

object UpdateCheckConstraints {
    /** Periodic checks may use battery constraints. */
    fun from(prefs: BackgroundUpdatePrefs): Constraints = Constraints.Builder()
        .setRequiredNetworkType(
            if (prefs.unmeteredOnly) NetworkType.UNMETERED else NetworkType.CONNECTED,
        )
        .setRequiresBatteryNotLow(true)
        .build()

    /**
     * Expedited one-shot work only allows network and storage constraints
     * (see WorkRequest.Builder.build). Never add battery/charging/idle here.
     */
    fun forExpeditedImmediate(unmeteredOnly: Boolean = false): Constraints = Constraints.Builder()
        .setRequiredNetworkType(
            if (unmeteredOnly) NetworkType.UNMETERED else NetworkType.CONNECTED,
        )
        .build()
}

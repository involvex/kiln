package dev.involvex.kiln.feature.sources.updatecheck

import androidx.work.Constraints
import androidx.work.NetworkType
import dev.involvex.kiln.core.model.BackgroundUpdatePrefs

object UpdateCheckConstraints {
    fun from(prefs: BackgroundUpdatePrefs): Constraints = Constraints.Builder()
        .setRequiredNetworkType(
            if (prefs.unmeteredOnly) NetworkType.UNMETERED else NetworkType.CONNECTED,
        )
        .setRequiresBatteryNotLow(true)
        .build()
}

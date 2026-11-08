package dev.involvex.kiln.feature.sources.updatecheck

import androidx.work.NetworkType
import dev.involvex.kiln.core.model.BackgroundUpdatePrefs
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UpdateCheckConstraintsTest {
    @Test
    fun unmetered_only_uses_unmetered_network() {
        val constraints = UpdateCheckConstraints.from(
            BackgroundUpdatePrefs(enabled = true, unmeteredOnly = true),
        )
        assertEquals(NetworkType.UNMETERED, constraints.requiredNetworkType)
        assertTrue(constraints.requiresBatteryNotLow())
    }

    @Test
    fun unmetered_off_uses_connected() {
        val constraints = UpdateCheckConstraints.from(
            BackgroundUpdatePrefs(enabled = true, unmeteredOnly = false),
        )
        assertEquals(NetworkType.CONNECTED, constraints.requiredNetworkType)
    }
}

package dev.involvex.kiln.feature.apps.activities

import android.content.pm.ActivityInfo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AppActivityLabelsTest {
    @Test
    fun launchModeKeys() {
        assertEquals("standard", AppActivityLabels.launchModeKey(ActivityInfo.LAUNCH_MULTIPLE))
        assertEquals("singleTop", AppActivityLabels.launchModeKey(ActivityInfo.LAUNCH_SINGLE_TOP))
        assertEquals("singleTask", AppActivityLabels.launchModeKey(ActivityInfo.LAUNCH_SINGLE_TASK))
        assertEquals("singleInstance", AppActivityLabels.launchModeKey(ActivityInfo.LAUNCH_SINGLE_INSTANCE))
        assertEquals("unknown", AppActivityLabels.launchModeKey(-1))
    }

    @Test
    fun flagKeys_knownBits() {
        val flags = ActivityInfo.FLAG_EXCLUDE_FROM_RECENTS or
            ActivityInfo.FLAG_HARDWARE_ACCELERATED or
            ActivityInfo.FLAG_NO_HISTORY
        assertEquals(
            listOf("excludeRecent", "hardwareAccel", "noHistory"),
            AppActivityLabels.flagKeys(flags),
        )
    }

    @Test
    fun matchesQuery_labelAndClass() {
        val info = AppActivityInfoQuery(label = "Termux Activity", className = "com.invapp.app.TermuxActivity")
        assertTrue(AppActivityLabels.matchesQuery(info, ""))
        assertTrue(AppActivityLabels.matchesQuery(info, "termux"))
        assertTrue(AppActivityLabels.matchesQuery(info, "TermuxActivity"))
        assertFalse(AppActivityLabels.matchesQuery(info, "debloat"))
    }
}

package dev.involvex.kiln.automation

import dev.involvex.kiln.core.model.AutomationApi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AutomationApiConstantsTest {
    @Test
    fun stableNamesMatchPlan() {
        assertEquals("dev.involvex.kiln.permission.AUTOMATION", AutomationApi.PERMISSION)
        assertEquals("dev.involvex.kiln.action.CHECK_UPDATES", AutomationApi.ACTION_CHECK_UPDATES)
        assertEquals("dev.involvex.kiln.action.INSTALL_UPDATE", AutomationApi.ACTION_INSTALL_UPDATE)
        assertEquals("dev.involvex.kiln.action.INSTALL_FROM_URL", AutomationApi.ACTION_INSTALL_FROM_URL)
        assertEquals("dev.involvex.kiln.action.CLEAN_CACHE", AutomationApi.ACTION_CLEAN_CACHE)
        assertEquals("dev.involvex.kiln.action.AUTOMATION_RESULT", AutomationApi.ACTION_RESULT)
        assertTrue(AutomationApi.ACTION_CHECK_UPDATES.startsWith("dev.involvex.kiln.action."))
    }
}

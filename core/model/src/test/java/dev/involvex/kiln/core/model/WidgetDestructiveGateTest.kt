package dev.involvex.kiln.core.model

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WidgetDestructiveGateTest {
    @Test
    fun `cache clear requires pending confirm`() {
        assertFalse(CacheClearConfirmGate.mayExecute(confirmPending = false))
        assertTrue(CacheClearConfirmGate.mayExecute(confirmPending = true))
    }

    @Test
    fun `force stop requires matching pending package`() {
        assertFalse(
            ForceStopConfirmGate.mayExecute(
                pendingPackage = null,
                targetPackage = "com.example.app",
                kilnPackageName = "dev.involvex.kiln",
            ),
        )
        assertFalse(
            ForceStopConfirmGate.mayExecute(
                pendingPackage = "com.other.app",
                targetPackage = "com.example.app",
                kilnPackageName = "dev.involvex.kiln",
            ),
        )
        assertTrue(
            ForceStopConfirmGate.mayExecute(
                pendingPackage = "com.example.app",
                targetPackage = "com.example.app",
                kilnPackageName = "dev.involvex.kiln",
            ),
        )
    }

    @Test
    fun `force stop never allows kiln itself`() {
        assertFalse(
            ForceStopConfirmGate.mayExecute(
                pendingPackage = "dev.involvex.kiln",
                targetPackage = "dev.involvex.kiln",
                kilnPackageName = "dev.involvex.kiln",
            ),
        )
    }
}

package dev.involvex.kiln.feature.installer

import dev.involvex.kiln.feature.installer.session.InstallStatusMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class InstallStatusMapperTest {
    @Test
    fun `maps known statuses`() {
        // PackageInstaller status ints (framework constants).
        assertEquals("install_success", InstallStatusMapper.messageKey(STATUS_SUCCESS))
        assertEquals("install_failure_conflict", InstallStatusMapper.messageKey(STATUS_FAILURE_CONFLICT))
        assertEquals("install_pending_user_action", InstallStatusMapper.messageKey(STATUS_PENDING_USER_ACTION))
    }

    @Test
    fun `pending user action is not terminal failure`() {
        assertFalse(InstallStatusMapper.isTerminalFailure(STATUS_PENDING_USER_ACTION))
        assertFalse(InstallStatusMapper.isTerminalFailure(STATUS_SUCCESS))
        assertTrue(InstallStatusMapper.isTerminalFailure(STATUS_FAILURE))
    }

    private companion object {
        const val STATUS_PENDING_USER_ACTION = -1
        const val STATUS_SUCCESS = 0
        const val STATUS_FAILURE = 1
        const val STATUS_FAILURE_CONFLICT = 5
    }
}

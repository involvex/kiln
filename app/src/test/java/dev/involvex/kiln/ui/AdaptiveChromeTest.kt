package dev.involvex.kiln.ui

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AdaptiveChromeTest {
    @Test
    fun useNavigationRail_falseBelowMedium() {
        assertFalse(useNavigationRail(359))
        assertFalse(useNavigationRail(599))
    }

    @Test
    fun useNavigationRail_trueAtMediumAndAbove() {
        assertTrue(useNavigationRail(600))
        assertTrue(useNavigationRail(840))
        assertTrue(useNavigationRail(1200))
    }
}

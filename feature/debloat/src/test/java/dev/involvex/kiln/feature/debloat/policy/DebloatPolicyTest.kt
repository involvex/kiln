package dev.involvex.kiln.feature.debloat.policy

import dev.involvex.kiln.core.model.DebloatAction
import dev.involvex.kiln.core.model.DebloatAppRow
import dev.involvex.kiln.core.model.DebloatPackageState
import dev.involvex.kiln.core.model.DebloatRating
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DebloatPolicyTest {
    private fun row(
        rating: DebloatRating = DebloatRating.RECOMMENDED,
        state: DebloatPackageState = DebloatPackageState.ENABLED,
        denylisted: Boolean = false,
    ) = DebloatAppRow(
        packageName = "com.example.app",
        label = "Example",
        isSystem = true,
        state = state,
        rating = rating,
        category = null,
        description = null,
        breaks = emptyList(),
        denylisted = denylisted,
        denylistReason = if (denylisted) "hard" else null,
    )

    @Test
    fun denylist_and_unsafe_never_selectable() {
        assertFalse(DebloatPolicy.canSelect(row(denylisted = true), DebloatAction.DISABLE, true))
        assertFalse(DebloatPolicy.canSelect(row(DebloatRating.UNSAFE), DebloatAction.DISABLE, true))
    }

    @Test
    fun unknown_and_expert_need_expert_mode() {
        assertFalse(DebloatPolicy.canSelect(row(DebloatRating.UNKNOWN), DebloatAction.DISABLE, false))
        assertTrue(DebloatPolicy.canSelect(row(DebloatRating.UNKNOWN), DebloatAction.DISABLE, true))
        assertFalse(DebloatPolicy.canSelect(row(DebloatRating.EXPERT), DebloatAction.DISABLE, false))
        assertTrue(DebloatPolicy.canSelect(row(DebloatRating.EXPERT), DebloatAction.DISABLE, true))
    }

    @Test
    fun recommended_disable_when_enabled() {
        assertTrue(DebloatPolicy.canSelect(row(), DebloatAction.DISABLE, false))
        assertFalse(DebloatPolicy.canSelect(row(state = DebloatPackageState.DISABLED), DebloatAction.DISABLE, false))
        assertTrue(
            DebloatPolicy.canSelect(
                row(state = DebloatPackageState.DISABLED),
                DebloatAction.ENABLE,
                false,
            ),
        )
    }
}

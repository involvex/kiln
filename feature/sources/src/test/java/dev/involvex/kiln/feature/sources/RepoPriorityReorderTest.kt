package dev.involvex.kiln.feature.sources

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class RepoPriorityReorderTest {
    @Test
    fun move_up_swaps_with_previous() {
        val result = RepoPriorityReorder.afterMove(
            orderedIds = listOf("izzy", "fdroid", "guardian"),
            moveId = "fdroid",
            delta = -1,
        )
        assertEquals(
            listOf("fdroid" to 0, "izzy" to 1, "guardian" to 2),
            result,
        )
    }

    @Test
    fun move_down_at_end_is_noop() {
        assertNull(
            RepoPriorityReorder.afterMove(
                orderedIds = listOf("a", "b"),
                moveId = "b",
                delta = 1,
            ),
        )
    }

    @Test
    fun unknown_id_is_noop() {
        assertNull(RepoPriorityReorder.afterMove(listOf("a"), moveId = "x", delta = -1))
    }
}

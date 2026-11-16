package dev.involvex.kiln.feature.installer.rollback

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RollbackKeepPolicyTest {
    @Test
    fun keep_2_drops_older_of_four() {
        val newestFirst = listOf(40L, 30L, 20L, 10L)
        assertEquals(listOf(20L, 10L), RollbackKeepPolicy.versionCodesToDelete(newestFirst, keep = 2))
    }

    @Test
    fun keep_all_when_under_limit() {
        val newestFirst = listOf(3L, 2L)
        assertEquals(emptyList<Long>(), RollbackKeepPolicy.versionCodesToDelete(newestFirst, keep = 3))
    }

    @Test
    fun keep_zero_deletes_all() {
        assertEquals(listOf(2L, 1L), RollbackKeepPolicy.versionCodesToDelete(listOf(2L, 1L), keep = 0))
    }
}

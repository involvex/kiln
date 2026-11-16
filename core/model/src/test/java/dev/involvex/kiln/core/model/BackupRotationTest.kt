package dev.involvex.kiln.core.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BackupRotationTest {
    @Test
    fun keepsNewest_deletesOldest() {
        val entries = listOf(
            BackupTreeEntry("a.zip", "a", 100),
            BackupTreeEntry("b.zip", "b", 300),
            BackupTreeEntry("c.zip", "c", 200),
            BackupTreeEntry("d.zip", "d", 400),
        )
        val delete = BackupRotation.selectForDeletion(entries, keepN = 2)
        assertEquals(listOf("c", "a"), delete.map { it.uriString })
    }

    @Test
    fun underLimit_deletesNothing() {
        val entries = listOf(BackupTreeEntry("a.zip", "a", 1))
        assertEquals(emptyList<BackupTreeEntry>(), BackupRotation.selectForDeletion(entries, 3))
    }
}

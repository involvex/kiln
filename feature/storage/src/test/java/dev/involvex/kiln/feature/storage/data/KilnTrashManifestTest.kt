package dev.involvex.kiln.feature.storage.data

import dev.involvex.kiln.core.model.KilnTrashEntry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class KilnTrashManifestTest {
    @Test
    fun round_trip_preserves_entries_and_escapes() {
        val entries = listOf(
            KilnTrashEntry(
                id = "id1",
                originalRelativePath = "folder/a\tb.bin",
                trashFileName = "uuid_a.bin",
                displayName = "a\tb.bin",
                sizeBytes = 42L,
                movedAtEpochMs = 100L,
                uriString = "content://trash/uuid_a.bin",
            ),
        )
        val encoded = KilnTrashManifest.encode(entries)
        val decoded = KilnTrashManifest.decode(encoded) { name ->
            "content://trash/$name"
        }
        assertEquals(1, decoded.size)
        assertEquals("folder/a\tb.bin", decoded[0].originalRelativePath)
        assertEquals("a\tb.bin", decoded[0].displayName)
        assertEquals(42L, decoded[0].sizeBytes)
        assertEquals("content://trash/uuid_a.bin", decoded[0].uriString)
    }
}

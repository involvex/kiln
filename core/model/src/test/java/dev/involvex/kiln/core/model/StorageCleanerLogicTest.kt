package dev.involvex.kiln.core.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StorageCleanerLogicTest {
    @Test
    fun largeFiles_applies_threshold_excludes_apk_leftovers_and_sorts() {
        val threshold = StorageCleanerLogic.LARGE_FILE_THRESHOLD_BYTES
        val files = listOf(
            file("big.bin", threshold + 10, "a/big.bin"),
            file("mid.bin", threshold - 1, "a/mid.bin"),
            file("huge.apk", threshold + 100, "a/huge.apk"),
            file("bigger.bin", threshold + 50, "a/bigger.bin"),
        )
        val result = StorageCleanerLogic.largeFiles(files, thresholdBytes = threshold, limit = 10)
        assertEquals(listOf("bigger.bin", "big.bin"), result.map { it.displayName })
    }

    @Test
    fun sameSizeGroups_keeps_only_groups_of_two_or_more() {
        val files = listOf(
            file("a.bin", 100, "a.bin"),
            file("b.bin", 100, "b.bin"),
            file("c.bin", 200, "c.bin"),
            file("d.bin", 0, "d.bin"),
        )
        val groups = StorageCleanerLogic.sameSizeGroups(files)
        assertEquals(setOf(100L), groups.keys)
        assertEquals(2, groups.getValue(100L).size)
    }

    @Test
    fun duplicateGroupsFromHashes_pairs_matching_digests() {
        val a = file("a.bin", 100, "a.bin")
        val b = file("b.bin", 100, "b.bin")
        val c = file("c.bin", 100, "c.bin")
        val groups = StorageCleanerLogic.duplicateGroupsFromHashes(
            listOf(a to "AAA", b to "aaa", c to "bbb"),
        )
        assertEquals(1, groups.size)
        assertEquals("aaa", groups[0].sha256)
        assertEquals(listOf("a.bin", "b.bin"), groups[0].files.map { it.displayName })
    }

    @Test
    fun hashCandidates_caps_and_prefers_larger_sizes() {
        val sameSize = mapOf(
            300L to listOf(file("c1", 300, "c1"), file("c2", 300, "c2")),
            100L to listOf(file("a1", 100, "a1"), file("a2", 100, "a2")),
        )
        val candidates = StorageCleanerLogic.hashCandidates(sameSize, maxFiles = 3)
        assertEquals(3, candidates.size)
        assertTrue(candidates.all { it.sizeBytes == 300L || it.sizeBytes == 100L })
        assertEquals(2, candidates.count { it.sizeBytes == 300L })
    }

    private fun file(name: String, size: Long, path: String) = SafTreeFile(
        id = path,
        displayName = name,
        sizeBytes = size,
        uriString = "content://tree/$path",
        relativePath = path,
    )
}

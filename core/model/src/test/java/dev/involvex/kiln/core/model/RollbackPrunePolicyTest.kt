package dev.involvex.kiln.core.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RollbackPrunePolicyTest {
    @Test
    fun deletes_entries_older_than_max_age() {
        val now = 1_000_000L
        val old = entry("a", versionCode = 1, savedAt = now - 10_000, size = 100)
        val fresh = entry("a", versionCode = 2, savedAt = now - 100, size = 100)
        val deleted = RollbackPrunePolicy.selectForDeletion(
            entries = listOf(old, fresh),
            maxAgeMs = 1_000,
            maxTotalBytes = 10_000,
            nowEpochMs = now,
        )
        assertEquals(listOf(old), deleted)
    }

    @Test
    fun deletes_oldest_until_under_size_cap() {
        val now = 1_000_000L
        val a = entry("a", 1, now - 10, 400)
        val b = entry("b", 1, now - 20, 400)
        val c = entry("c", 1, now - 30, 400)
        val deleted = RollbackPrunePolicy.selectForDeletion(
            entries = listOf(a, b, c),
            maxAgeMs = 1_000_000,
            maxTotalBytes = 500,
            nowEpochMs = now,
        )
        assertTrue(deleted.contains(c))
        assertTrue(deleted.contains(b))
        assertEquals(2, deleted.size)
    }

    private fun entry(pkg: String, versionCode: Long, savedAt: Long, size: Long) = RollbackApkEntry(
        packageName = pkg,
        versionName = versionCode.toString(),
        versionCode = versionCode,
        apkAbsolutePath = "/tmp/$pkg-$versionCode.apk",
        savedAtEpochMs = savedAt,
        sizeBytes = size,
    )
}

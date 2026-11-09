package dev.involvex.kiln.feature.maintenance

import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class CacheDirJanitorTest {
    @TempDir
    lateinit var temp: File

    @Test
    fun pruneFilesOlderThan_deletes_stale_only() {
        val oldFile = File(temp, "old.bin").apply {
            writeText("hello")
            setLastModified(1_000L)
        }
        val newFile = File(temp, "new.bin").apply {
            writeText("world!")
            setLastModified(50_000L)
        }
        val freed = CacheDirJanitor.pruneFilesOlderThan(temp, maxAgeMs = 10_000L, nowEpochMs = 20_000L)
        assertEquals(5L, freed)
        assertFalse(oldFile.exists())
        assertTrue(newFile.exists())
    }

    @Test
    fun deleteContents_removes_nested_files() {
        val nested = File(temp, "sub").apply { mkdirs() }
        File(nested, "a.txt").writeText("abc")
        val freed = CacheDirJanitor.deleteContents(temp)
        assertEquals(3L, freed)
        assertTrue(temp.isDirectory)
        assertEquals(0, temp.listFiles()?.size ?: 0)
    }
}

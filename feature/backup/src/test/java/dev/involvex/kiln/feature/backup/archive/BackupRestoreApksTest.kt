package dev.involvex.kiln.feature.backup.archive

import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class BackupRestoreApksTest {
    @TempDir
    lateinit var temp: File

    @Test
    fun orderedAbsolutePaths_puts_base_first_then_sorted_splits() {
        val base = File(temp, "base.apk").apply { writeText("base") }
        val config = File(temp, "config.arm64_v8a.apk").apply { writeText("cfg") }
        val extra = File(temp, "split_foo.apk").apply { writeText("foo") }
        val paths = BackupRestoreApks.orderedAbsolutePaths(base, listOf(extra, base, config))
        assertEquals(
            listOf(base.absolutePath, config.absolutePath, extra.absolutePath),
            paths,
        )
    }

    @Test
    fun isIncompleteSplitArchive_detects_single_apk_when_hasSplits() {
        assertTrue(BackupRestoreApks.isIncompleteSplitArchive(hasSplits = true, apkCount = 1))
        assertFalse(BackupRestoreApks.isIncompleteSplitArchive(hasSplits = true, apkCount = 2))
        assertFalse(BackupRestoreApks.isIncompleteSplitArchive(hasSplits = false, apkCount = 1))
    }
}

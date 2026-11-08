package dev.involvex.kiln.feature.termux.run

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TermuxPathsTest {
    @Test
    fun officialLayout() {
        assertEquals("/data/data/com.termux/files", TermuxPaths.filesDir("com.termux"))
        assertEquals("/data/data/com.termux/files/usr", TermuxPaths.prefixDir("com.termux"))
        assertEquals("/data/data/com.termux/files/usr/bin", TermuxPaths.binDir("com.termux"))
        assertEquals("/data/data/com.termux/files/home", TermuxPaths.homeDir("com.termux"))
        assertEquals(
            "/data/data/com.termux/files/usr/bin/bash",
            TermuxPaths.executable("com.termux", "bash"),
        )
    }

    @Test
    fun forkLayoutUsesPackageId() {
        val id = "dev.example.termux"
        assertEquals("/data/data/$id/files/home", TermuxPaths.homeDir(id))
        assertEquals("/data/data/$id/files/usr/bin/rclone", TermuxPaths.executable(id, "rclone"))
    }
}

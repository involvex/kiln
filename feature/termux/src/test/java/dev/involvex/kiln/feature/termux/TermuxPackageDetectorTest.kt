package dev.involvex.kiln.feature.termux

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class TermuxPackageDetectorTest {
    @Test
    fun matchesOfficialTermuxPackageId() {
        assertEquals(
            "com.termux",
            TermuxPackageDetector.selectCandidate(listOf("com.termux")),
        )
    }

    @Test
    fun matchesRenamedFork() {
        assertEquals(
            "com.involvex.termux_app",
            TermuxPackageDetector.selectCandidate(
                listOf(
                    "dev.involvex.kiln.debug",
                    "com.involvex.termux_app",
                    "com.termux.boot",
                ),
            ),
        )
    }

    @Test
    fun matchesCaseInsensitiveSubstring() {
        assertEquals(
            "com.example.TERMUX_X",
            TermuxPackageDetector.selectCandidate(listOf("com.example.TERMUX_X")),
        )
    }

    @Test
    fun returnsNullWhenNoTermuxInstall() {
        assertNull(
            TermuxPackageDetector.selectCandidate(
                listOf("dev.involvex.kiln.debug", "com.android.settings"),
            ),
        )
    }

    @Test
    fun returnsFirstMatch() {
        assertEquals(
            "com.termux.boot",
            TermuxPackageDetector.selectCandidate(
                listOf("com.termux.boot", "com.termux"),
            ),
        )
    }
}

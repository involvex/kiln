package dev.involvex.kiln.core.common

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ApkSigningReaderTest {
    @Test
    fun fingerprintsMatch_ignoresColonCase() {
        val a = listOf("AB:CD:EF")
        val b = listOf("ab:cd:ef")
        assertTrue(ApkSigningReader.fingerprintsMatch(a, b))
    }

    @Test
    fun fingerprintsMatch_falseWhenDisjoint() {
        assertFalse(
            ApkSigningReader.fingerprintsMatch(
                listOf("AA:BB"),
                listOf("CC:DD"),
            ),
        )
    }

    @Test
    fun fingerprintsMatch_falseWhenEitherEmpty() {
        assertFalse(ApkSigningReader.fingerprintsMatch(emptyList(), listOf("AA")))
        assertFalse(ApkSigningReader.fingerprintsMatch(listOf("AA"), emptyList()))
    }
}

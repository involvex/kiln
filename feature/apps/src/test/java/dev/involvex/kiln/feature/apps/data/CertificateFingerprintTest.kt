package dev.involvex.kiln.feature.apps.data

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CertificateFingerprintTest {
    @Test
    fun `formats sha256 as uppercase colon separated hex`() {
        val bytes = byteArrayOf(0x0A, 0x1B.toByte(), 0xFF.toByte(), 0x00)
        // Full SHA-256 of these bytes:
        val fingerprint = sha256CertificateFingerprint(bytes)
        assertTrue(fingerprint.matches(Regex("([0-9A-F]{2}:){31}[0-9A-F]{2}")))
        assertEquals(fingerprint.uppercase(), fingerprint)
        assertTrue(':' in fingerprint)
    }

    @Test
    fun `rejects empty certificate bytes`() {
        assertThrows(IllegalArgumentException::class.java) {
            sha256CertificateFingerprint(byteArrayOf())
        }
    }
}

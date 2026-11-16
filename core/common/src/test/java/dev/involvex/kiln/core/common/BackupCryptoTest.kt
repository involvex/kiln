package dev.involvex.kiln.core.common

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BackupCryptoTest {
    @Test
    fun roundTrip_ok() {
        val plain = "hello-kiln-backup".toByteArray()
        val password = "s3cret!".toCharArray()
        val blob = BackupCrypto.encrypt(plain, password)
        assertTrue(BackupCrypto.looksEncrypted(blob))
        assertArrayEquals(plain, BackupCrypto.decrypt(blob, password))
    }

    @Test
    fun wrongPassword_fails() {
        val blob = BackupCrypto.encrypt(byteArrayOf(1, 2, 3), "right".toCharArray())
        assertThrows(Exception::class.java) {
            BackupCrypto.decrypt(blob, "wrong".toCharArray())
        }
    }

    @Test
    fun plainZip_notEncrypted() {
        assertFalse(BackupCrypto.looksEncrypted("PK\u0003\u0004".toByteArray()))
    }
}

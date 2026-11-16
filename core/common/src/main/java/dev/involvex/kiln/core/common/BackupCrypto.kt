package dev.involvex.kiln.core.common

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-GCM wrapper for Kiln backup archives (password + PBKDF2-HMAC-SHA256).
 * Pure JVM — unit-tested. Outer layout: magic + version + iterations + salt + iv + ciphertext+tag.
 */
object BackupCrypto {
    const val MAGIC = "KILNBK01"
    private const val VERSION: Byte = 1
    private const val SALT_LEN = 16
    private const val IV_LEN = 12
    private const val KEY_LEN_BITS = 256
    private const val TAG_LEN_BITS = 128
    private const val ITERATIONS_FIELD_BYTES = 4
    private const val MIN_ITERATIONS = 10_000
    private const val MAX_ITERATIONS = 2_000_000
    const val DEFAULT_ITERATIONS = 120_000

    fun looksEncrypted(header: ByteArray): Boolean {
        if (header.size < MAGIC.length) return false
        return header.copyOfRange(0, MAGIC.length).toString(Charsets.US_ASCII) == MAGIC
    }

    fun encrypt(plaintext: ByteArray, password: CharArray, iterations: Int = DEFAULT_ITERATIONS): ByteArray {
        require(password.isNotEmpty()) { "password_required" }
        val salt = ByteArray(SALT_LEN).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(IV_LEN).also { SecureRandom().nextBytes(it) }
        val key = deriveKey(password, salt, iterations)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_LEN_BITS, iv))
        val ciphertext = cipher.doFinal(plaintext)
        val out = ByteArrayOutputStream()
        out.write(MAGIC.toByteArray(Charsets.US_ASCII))
        out.write(byteArrayOf(VERSION))
        out.write(ByteBuffer.allocate(ITERATIONS_FIELD_BYTES).putInt(iterations).array())
        out.write(salt)
        out.write(iv)
        out.write(ciphertext)
        return out.toByteArray()
    }

    fun decrypt(blob: ByteArray, password: CharArray): ByteArray {
        require(password.isNotEmpty()) { "password_required" }
        if (!looksEncrypted(blob)) throw IllegalArgumentException("not_encrypted")
        var offset = MAGIC.length
        val version = blob[offset]
        offset += 1
        require(version == VERSION) { "unsupported_crypto_version" }
        val iterations = ByteBuffer.wrap(blob, offset, ITERATIONS_FIELD_BYTES).int
        offset += ITERATIONS_FIELD_BYTES
        require(iterations in MIN_ITERATIONS..MAX_ITERATIONS) { "invalid_iterations" }
        val salt = blob.copyOfRange(offset, offset + SALT_LEN)
        offset += SALT_LEN
        val iv = blob.copyOfRange(offset, offset + IV_LEN)
        offset += IV_LEN
        val ciphertext = blob.copyOfRange(offset, blob.size)
        val key = deriveKey(password, salt, iterations)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_LEN_BITS, iv))
        return try {
            cipher.doFinal(ciphertext)
        } catch (e: AEADBadTagException) {
            throw IllegalArgumentException("decrypt_failed", e)
        }
    }

    fun encryptStream(input: InputStream, output: OutputStream, password: CharArray) {
        val plain = input.readBytes()
        output.write(encrypt(plain, password))
    }

    fun decryptToStream(input: InputStream, password: CharArray): InputStream = ByteArrayInputStream(decrypt(input.readBytes(), password))

    private fun deriveKey(password: CharArray, salt: ByteArray, iterations: Int): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password, salt, iterations, KEY_LEN_BITS)
        val encoded = factory.generateSecret(spec).encoded
        return SecretKeySpec(encoded, "AES")
    }
}

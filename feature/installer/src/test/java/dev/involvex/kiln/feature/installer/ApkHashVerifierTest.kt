package dev.involvex.kiln.feature.installer

import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.feature.installer.verify.ApkHashVerifier
import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class ApkHashVerifierTest {
    @TempDir
    lateinit var dir: File

    @Test
    fun `accepts when expected hash is null`() {
        val file = File(dir, "a.apk").apply { writeText("hello") }
        assertTrue(ApkHashVerifier.verifySha256(file, null) is Result.Success)
    }

    @Test
    fun `rejects mismatch`() {
        val file = File(dir, "a.apk").apply { writeText("hello") }
        val result = ApkHashVerifier.verifySha256(file, "00".repeat(32))
        assertTrue(result is Result.Failure)
        assertEquals("apk_sha256_mismatch", (result as Result.Failure).message)
    }

    @Test
    fun `accepts matching hash`() {
        val file = File(dir, "a.apk").apply { writeText("hello") }
        val hex = ApkHashVerifier.sha256Hex(file)
        assertTrue(ApkHashVerifier.verifySha256(file, hex) is Result.Success)
    }
}

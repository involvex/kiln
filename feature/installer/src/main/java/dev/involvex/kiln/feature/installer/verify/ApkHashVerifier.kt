package dev.involvex.kiln.feature.installer.verify

import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.core.common.normalizeFingerprint
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

object ApkHashVerifier {
    fun verifySha256(file: File, expectedSha256: String?): Result<Unit> {
        if (expectedSha256.isNullOrBlank()) {
            return Result.success(Unit)
        }
        if (!file.exists() || file.length() == 0L) {
            return Result.failure("apk_missing")
        }
        val actual = sha256Hex(file)
        val expected = normalizeFingerprint(expectedSha256)
        return if (actual.equals(expected, ignoreCase = true)) {
            Result.success(Unit)
        } else {
            Result.failure("apk_sha256_mismatch")
        }
    }

    fun sha256Hex(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private const val DEFAULT_BUFFER = 8 * 1024
}

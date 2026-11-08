package dev.involvex.kiln.feature.sources.fdroid

import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.core.common.fingerprintsEqual
import dev.involvex.kiln.core.common.sha256CertificateFingerprint
import java.io.ByteArrayInputStream
import java.io.IOException
import java.security.cert.Certificate
import java.util.jar.JarEntry
import java.util.jar.JarInputStream

/**
 * Verifies that [entryJarBytes] is a signed JAR whose signer certificate SHA-256
 * matches [expectedFingerprint] (colons optional). Fail closed.
 */
object EntryJarVerifier {
    fun verify(entryJarBytes: ByteArray, expectedFingerprint: String): Result<ByteArray> {
        if (entryJarBytes.isEmpty()) {
            return Result.failure("empty_entry_jar")
        }
        return try {
            readVerifiedEntryJson(entryJarBytes, expectedFingerprint)
        } catch (error: IOException) {
            Result.failure(error.message ?: "entry_jar_verify_failed", error)
        } catch (error: SecurityException) {
            Result.failure(error.message ?: "entry_jar_verify_failed", error)
        } catch (error: IllegalArgumentException) {
            Result.failure(error.message ?: "entry_jar_verify_failed", error)
        }
    }

    private fun readVerifiedEntryJson(entryJarBytes: ByteArray, expectedFingerprint: String): Result<ByteArray> {
        JarInputStream(ByteArrayInputStream(entryJarBytes)).use { jar ->
            var entryJson: ByteArray? = null
            var matchedSigner = false
            var entry = jar.nextJarEntry
            while (entry != null) {
                val bytes = jar.readBytes()
                if (entry.name.equals("entry.json", ignoreCase = true)) {
                    entryJson = bytes
                }
                if (certificatesMatch(entry, expectedFingerprint)) {
                    matchedSigner = true
                }
                jar.closeEntry()
                entry = jar.nextJarEntry
            }
            if (!matchedSigner) {
                return Result.failure("entry_jar_signature_mismatch")
            }
            val json = entryJson ?: return Result.failure("entry_json_missing")
            return Result.success(json)
        }
    }

    private fun certificatesMatch(entry: JarEntry, expectedFingerprint: String): Boolean {
        val certs: Array<out Certificate>? = entry.certificates
        if (certs == null) return false
        return certs.any { cert ->
            fingerprintsEqual(sha256CertificateFingerprint(cert.encoded), expectedFingerprint)
        }
    }
}

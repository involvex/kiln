package dev.involvex.kiln.feature.sources.fdroid

import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.core.common.fingerprintsEqual
import dev.involvex.kiln.core.common.sha256CertificateFingerprint
import java.io.IOException
import java.nio.file.Files
import java.security.cert.Certificate
import java.util.jar.JarFile

/**
 * Verifies that [entryJarBytes] is a signed JAR whose signer certificate SHA-256
 * matches [expectedFingerprint] (colons optional). Fail closed.
 *
 * Uses [JarFile] with verification enabled. [java.util.jar.JarInputStream] does
 * not populate [java.util.jar.JarEntry.getCertificates] for F-Droid `entry.jar`
 * on Android/OpenJDK, so stream-based checks always fail closed incorrectly.
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
        val tmp = Files.createTempFile("kiln-entry-", ".jar").toFile()
        try {
            tmp.writeBytes(entryJarBytes)
            // verify=true: signature checked while entries are read
            JarFile(tmp, true).use { jar ->
                val entry = jar.getJarEntry(ENTRY_JSON)
                    ?: jar.getJarEntry(ENTRY_JSON_ALT)
                    ?: return Result.failure("entry_json_missing")
                val entryJson = jar.getInputStream(entry).use { it.readBytes() }
                // Certificates are only available after the entry stream is fully consumed.
                if (!certificatesMatch(entry.certificates, expectedFingerprint)) {
                    return Result.failure("entry_jar_signature_mismatch")
                }
                return Result.success(entryJson)
            }
        } finally {
            tmp.delete()
        }
    }

    private fun certificatesMatch(certs: Array<out Certificate>?, expectedFingerprint: String): Boolean {
        if (certs.isNullOrEmpty()) return false
        return certs.any { cert ->
            fingerprintsEqual(sha256CertificateFingerprint(cert.encoded), expectedFingerprint)
        }
    }

    private const val ENTRY_JSON = "entry.json"
    private const val ENTRY_JSON_ALT = "ENTRY.JSON"
}

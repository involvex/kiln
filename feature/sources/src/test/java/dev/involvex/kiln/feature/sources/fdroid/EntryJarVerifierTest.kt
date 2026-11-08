package dev.involvex.kiln.feature.sources.fdroid

import dev.involvex.kiln.core.common.Result
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

class EntryJarVerifierTest {
    @Test
    fun `empty bytes fail closed`() {
        val result = EntryJarVerifier.verify(ByteArray(0), "AA:BB")
        assertTrue(result is Result.Failure)
        assertEquals("empty_entry_jar", (result as Result.Failure).message)
    }

    @Test
    fun `unsigned jar with entry json is rejected`() {
        val jar = unsignedJarWithEntryJson("""{"index":{"name":"index-v2.json"}}""")
        val result = EntryJarVerifier.verify(jar, "AA:BB:CC:DD")
        assertTrue(result is Result.Failure)
        assertEquals("entry_jar_signature_mismatch", (result as Result.Failure).message)
    }

    @Test
    fun `real fdroid entry jar verifies against published fingerprint`() {
        val bytes = javaClass.classLoader
            ?.getResourceAsStream("fdroid/fdroid-entry.jar")
            ?.readBytes()
            ?: fail("Missing test fixture fdroid/fdroid-entry.jar")
        val result = EntryJarVerifier.verify(bytes, FDROID_FINGERPRINT)
        assertTrue(result is Result.Success, "verify failed: ${(result as? Result.Failure)?.message}")
        val json = (result as Result.Success).value.decodeToString()
        assertTrue(json.contains("index-v2.json") || json.contains("\"index\""))
    }

    @Test
    fun `real fdroid entry jar rejects wrong fingerprint`() {
        val bytes = javaClass.classLoader
            ?.getResourceAsStream("fdroid/fdroid-entry.jar")
            ?.readBytes()
            ?: fail("Missing test fixture fdroid/fdroid-entry.jar")
        val result = EntryJarVerifier.verify(bytes, "00".repeat(32))
        assertTrue(result is Result.Failure)
        assertEquals("entry_jar_signature_mismatch", (result as Result.Failure).message)
    }

    private fun unsignedJarWithEntryJson(json: String): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry("entry.json"))
            zip.write(json.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        return out.toByteArray()
    }

    private companion object {
        // Official F-Droid repo signer SHA-256 (no colons).
        const val FDROID_FINGERPRINT =
            "43238D512C1E5EB2D6569F4A3AFBF5523418B82E0A3ED1552770ABB9A9C9CCAB"
    }
}

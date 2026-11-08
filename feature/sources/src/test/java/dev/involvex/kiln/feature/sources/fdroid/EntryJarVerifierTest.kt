package dev.involvex.kiln.feature.sources.fdroid

import dev.involvex.kiln.core.common.Result
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

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

    private fun unsignedJarWithEntryJson(json: String): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry("entry.json"))
            zip.write(json.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        return out.toByteArray()
    }
}

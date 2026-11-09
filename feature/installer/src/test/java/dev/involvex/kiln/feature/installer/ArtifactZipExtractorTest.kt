package dev.involvex.kiln.feature.installer

import dev.involvex.kiln.core.common.Result
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class ArtifactZipExtractorTest {
    @TempDir
    lateinit var temp: File

    @Test
    fun `extracts apk matching regex`() {
        val zip = File(temp, "art.zip")
        ZipOutputStream(zip.outputStream()).use { zos ->
            zos.putNextEntry(ZipEntry("notes.txt"))
            zos.write("hi".toByteArray())
            zos.closeEntry()
            zos.putNextEntry(ZipEntry("out/app-debug.apk"))
            zos.write(byteArrayOf(0x50, 0x4B))
            zos.closeEntry()
        }
        val out = File(temp, "picked.apk")
        val result = ArtifactZipExtractor.extractMatchingApk(zip, out, Regex(".*\\.apk$"))
        assertTrue(result is Result.Success)
        assertEquals(2, out.length())
    }

    @Test
    fun `fails when no apk`() {
        val zip = File(temp, "empty.zip")
        ZipOutputStream(zip.outputStream()).use { zos ->
            zos.putNextEntry(ZipEntry("readme.md"))
            zos.write("x".toByteArray())
            zos.closeEntry()
        }
        val result = ArtifactZipExtractor.extractMatchingApk(zip, File(temp, "x.apk"), Regex(".*\\.apk$"))
        assertTrue(result is Result.Failure)
    }

    @Test
    fun `detects artifact zip urls`() {
        assertTrue(
            ArtifactZipExtractor.looksLikeArtifactZip(
                "https://api.github.com/repos/o/r/actions/artifacts/1/zip",
                requiresGitHubAuth = false,
            ),
        )
        assertTrue(
            ArtifactZipExtractor.looksLikeArtifactZip(
                "https://example.com/file.apk",
                requiresGitHubAuth = true,
            ),
        )
    }
}

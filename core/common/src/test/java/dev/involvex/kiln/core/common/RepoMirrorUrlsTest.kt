package dev.involvex.kiln.core.common

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RepoMirrorUrlsTest {
    @Test
    fun candidates_primaryThenMirrors_dedupesAndHttpsOnly() {
        val result = RepoMirrorUrls.candidates(
            address = "https://f-droid.org/repo/",
            mirrorsCsv = "https://mirror.example/repo,http://bad.example,https://f-droid.org/repo",
        )
        assertEquals(
            listOf("https://f-droid.org/repo", "https://mirror.example/repo"),
            result,
        )
    }

    @Test
    fun parseMirrorsInput_acceptsNewlinesAndCommas() {
        val csv = RepoMirrorUrls.parseMirrorsInput(
            """
            https://a.example/repo
            https://b.example/repo, https://c.example/repo
            ftp://nope
            """.trimIndent(),
        )
        assertEquals("https://a.example/repo,https://b.example/repo,https://c.example/repo", csv)
    }

    @Test
    fun alternateDownloadUrls_rewritesOntoEachBase() {
        val urls = RepoMirrorUrls.alternateDownloadUrls(
            primaryUrl = "https://f-droid.org/repo/app.apk",
            address = "https://f-droid.org/repo",
            mirrorsCsv = "https://mirror.example/repo",
        )
        assertEquals(
            listOf(
                "https://f-droid.org/repo/app.apk",
                "https://mirror.example/repo/app.apk",
            ),
            urls,
        )
    }

    @Test
    fun alternateDownloadUrls_unrelatedUrl_unchanged() {
        val urls = RepoMirrorUrls.alternateDownloadUrls(
            primaryUrl = "https://github.com/owner/repo/releases/download/1/app.apk",
            address = "https://f-droid.org/repo",
            mirrorsCsv = "https://mirror.example/repo",
        )
        assertEquals(
            listOf("https://github.com/owner/repo/releases/download/1/app.apk"),
            urls,
        )
    }
}

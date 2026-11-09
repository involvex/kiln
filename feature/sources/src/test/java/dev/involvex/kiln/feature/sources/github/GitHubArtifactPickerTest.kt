package dev.involvex.kiln.feature.sources.github

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class GitHubArtifactPickerTest {
    @Test
    fun `picks successful run with matching artifact name`() {
        val runs = listOf(
            GitHubWorkflowRunDto(id = 1, runNumber = 10, conclusion = "failure"),
            GitHubWorkflowRunDto(id = 2, runNumber = 11, conclusion = "success"),
        )
        val artifacts = mapOf(
            2L to listOf(
                GitHubArtifactDto(
                    id = 99,
                    name = "debug-symbols",
                    sizeInBytes = 1,
                    archiveDownloadUrl = "https://api.github.com/a/99/zip",
                ),
                GitHubArtifactDto(
                    id = 100,
                    name = "app-nightly",
                    sizeInBytes = 50,
                    archiveDownloadUrl = "https://api.github.com/a/100/zip",
                ),
            ),
        )
        val pick = GitHubArtifactPicker.pick(runs, artifacts)
        assertEquals(100L, pick?.artifactId)
        assertEquals("nightly-11", pick?.versionLabel)
        assertEquals("https://api.github.com/a/100/zip", pick?.downloadUrl)
    }

    @Test
    fun `falls back to first non-expired artifact when regex misses`() {
        val runs = listOf(GitHubWorkflowRunDto(id = 3, runNumber = 5, conclusion = "success"))
        val artifacts = mapOf(
            3L to listOf(
                GitHubArtifactDto(
                    id = 7,
                    name = "something-else",
                    sizeInBytes = 9,
                    archiveDownloadUrl = "https://api.github.com/a/7/zip",
                ),
            ),
        )
        val pick = GitHubArtifactPicker.pick(runs, artifacts, Regex("(?i).*apk.*"))
        assertEquals(7L, pick?.artifactId)
    }

    @Test
    fun `skips expired artifacts`() {
        val runs = listOf(GitHubWorkflowRunDto(id = 4, runNumber = 1, conclusion = "success"))
        val artifacts = mapOf(
            4L to listOf(
                GitHubArtifactDto(
                    id = 1,
                    name = "apk",
                    sizeInBytes = 1,
                    archiveDownloadUrl = "https://api.github.com/a/1/zip",
                    expired = true,
                ),
            ),
        )
        assertNull(GitHubArtifactPicker.pick(runs, artifacts))
    }
}

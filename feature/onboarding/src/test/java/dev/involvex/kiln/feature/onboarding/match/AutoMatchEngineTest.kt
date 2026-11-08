package dev.involvex.kiln.feature.onboarding.match

import dev.involvex.kiln.core.database.IndexedPackageEntity
import dev.involvex.kiln.core.model.InstalledAppMatchInput
import dev.involvex.kiln.core.model.MatchConfidence
import dev.involvex.kiln.feature.onboarding.github.GitHubMap
import dev.involvex.kiln.feature.onboarding.github.GitHubMapEntry
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AutoMatchEngineTest {
    @Test
    fun `high when signer matches`() {
        val githubMap = mockk<GitHubMap>()
        every { githubMap.lookup(any()) } returns null
        val installed = listOf(
            InstalledAppMatchInput(
                packageName = "com.example",
                label = "Example",
                versionName = "1",
                versionCode = 1,
                signerSha256 = listOf("AA:BB"),
            ),
        )
        val indexed = listOf(
            IndexedPackageEntity(
                packageName = "com.example",
                repoId = "fdroid",
                name = "Example",
                summary = null,
                sourceCodeUrl = null,
                signerSha256Csv = "AA:BB",
                suggestedVersionCode = 2,
                suggestedVersionName = "2",
            ),
        )
        val result = AutoMatchEngine.match(
            installed = installed,
            indexed = indexed,
            repoPriority = mapOf("fdroid" to 1),
            repoNames = mapOf("fdroid" to "F-Droid"),
            githubMap = githubMap,
        )
        assertEquals(MatchConfidence.HIGH, result.single().confidence)
    }

    @Test
    fun `medium when github map only`() {
        val githubMap = mockk<GitHubMap>()
        every { githubMap.lookup("com.mapped") } returns
            GitHubMapEntry(packageName = "com.mapped", ownerRepo = "org/app", assetRegex = ".*\\.apk$")
        every { githubMap.lookup(match { it != "com.mapped" }) } returns null
        val installed = listOf(
            InstalledAppMatchInput(
                packageName = "com.mapped",
                label = "Mapped",
                versionName = "1",
                versionCode = 1,
                signerSha256 = listOf("AA:BB"),
            ),
        )
        val result = AutoMatchEngine.match(
            installed = installed,
            indexed = emptyList(),
            repoPriority = emptyMap(),
            repoNames = emptyMap(),
            githubMap = githubMap,
        )
        assertEquals(MatchConfidence.MEDIUM, result.single().confidence)
        assertEquals("org/app", result.single().githubOwnerRepo)
    }

    @Test
    fun `none when no index and no github map`() {
        val githubMap = mockk<GitHubMap>()
        every { githubMap.lookup(any()) } returns null
        val installed = listOf(
            InstalledAppMatchInput(
                packageName = "com.unknown",
                label = "Unknown",
                versionName = "1",
                versionCode = 1,
                signerSha256 = emptyList(),
            ),
        )
        val result = AutoMatchEngine.match(
            installed = installed,
            indexed = emptyList(),
            repoPriority = emptyMap(),
            repoNames = emptyMap(),
            githubMap = githubMap,
        )
        assertEquals(MatchConfidence.NONE, result.single().confidence)
    }

    @Test
    fun `warning when signer mismatches`() {
        val githubMap = mockk<GitHubMap>()
        every { githubMap.lookup(any()) } returns null
        val installed = listOf(
            InstalledAppMatchInput(
                packageName = "com.example",
                label = "Example",
                versionName = "1",
                versionCode = 1,
                signerSha256 = listOf("11:22"),
            ),
        )
        val indexed = listOf(
            IndexedPackageEntity(
                packageName = "com.example",
                repoId = "fdroid",
                name = "Example",
                summary = null,
                sourceCodeUrl = null,
                signerSha256Csv = "AA:BB",
                suggestedVersionCode = 2,
                suggestedVersionName = "2",
            ),
        )
        val result = AutoMatchEngine.match(
            installed = installed,
            indexed = indexed,
            repoPriority = mapOf("fdroid" to 1),
            repoNames = mapOf("fdroid" to "F-Droid"),
            githubMap = githubMap,
        )
        assertEquals(MatchConfidence.WARNING, result.single().confidence)
    }
}

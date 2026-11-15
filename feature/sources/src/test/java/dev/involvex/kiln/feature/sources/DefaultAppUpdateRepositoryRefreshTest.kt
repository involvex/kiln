package dev.involvex.kiln.feature.sources

import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.core.database.IndexDao
import dev.involvex.kiln.core.database.ManagedAppDao
import dev.involvex.kiln.core.database.ManagedAppEntity
import dev.involvex.kiln.core.database.RepoDao
import dev.involvex.kiln.core.model.GitHubReleaseChannel
import dev.involvex.kiln.core.model.InstalledAppMatchInput
import dev.involvex.kiln.core.model.InstalledAppsReader
import dev.involvex.kiln.feature.sources.fdroid.FDroidRepoSync
import dev.involvex.kiln.feature.sources.github.GitHubActionsClient
import dev.involvex.kiln.feature.sources.github.GitHubReleasePick
import dev.involvex.kiln.feature.sources.github.GitHubReleasesClient
import dev.involvex.kiln.feature.sources.github.GitHubTokenStore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DefaultAppUpdateRepositoryRefreshTest {
    private lateinit var managedAppDao: ManagedAppDao
    private lateinit var indexDao: IndexDao
    private lateinit var repoDao: RepoDao
    private lateinit var installedAppsReader: InstalledAppsReader
    private lateinit var gitHubReleasesClient: GitHubReleasesClient
    private lateinit var gitHubActionsClient: GitHubActionsClient
    private lateinit var gitHubTokenStore: GitHubTokenStore
    private lateinit var fdroidRepoSync: FDroidRepoSync
    private lateinit var repository: DefaultAppUpdateRepository

    @BeforeEach
    fun setUp() {
        managedAppDao = mockk(relaxed = true)
        indexDao = mockk(relaxed = true)
        repoDao = mockk(relaxed = true)
        installedAppsReader = mockk()
        gitHubReleasesClient = mockk()
        gitHubActionsClient = mockk(relaxed = true)
        gitHubTokenStore = mockk(relaxed = true)
        fdroidRepoSync = mockk(relaxed = true)
        repository = DefaultAppUpdateRepository(
            managedAppDao = managedAppDao,
            indexDao = indexDao,
            repoDao = repoDao,
            installedAppsReader = installedAppsReader,
            gitHubReleasesClient = gitHubReleasesClient,
            gitHubActionsClient = gitHubActionsClient,
            gitHubTokenStore = gitHubTokenStore,
            fdroidRepoSync = fdroidRepoSync,
        )
        coEvery { managedAppDao.getAll() } returns listOf(
            ManagedAppEntity(
                packageName = PKG,
                sourceId = "github:owner/repo",
                confidence = "MEDIUM",
                pinned = true,
                ignoreUpdates = false,
                githubOwnerRepo = "owner/repo",
                githubAssetRegex = ".*\\.apk$",
                githubChannel = "STABLE",
            ),
        )
        coEvery { installedAppsReader.userApps() } returns listOf(
            InstalledAppMatchInput(
                packageName = PKG,
                label = "App",
                versionName = "1.0",
                versionCode = 1L,
                signerSha256 = emptyList(),
            ),
        )
        coEvery { repoDao.getAll() } returns emptyList()
        coEvery { indexDao.releasesFor(PKG) } returns emptyList()
    }

    @Test
    fun refreshUpdates_doesNotCallGitHubClient() = runBlocking {
        repository.refreshUpdates()
        coVerify(exactly = 0) {
            gitHubReleasesClient.latestMatchingRelease(any(), any(), any())
        }
        coVerify(exactly = 0) {
            gitHubActionsClient.latestMatchingArtifact(any())
        }
    }

    @Test
    fun checkForUpdates_probesGitHubAfterRepoSync() = runBlocking {
        coEvery {
            gitHubReleasesClient.latestMatchingRelease(any(), any(), any())
        } returns Result.success(
            GitHubReleasePick(
                tagName = "v2.0",
                assetName = "app.apk",
                downloadUrl = "https://example.com/app.apk",
                sizeBytes = 10L,
                releaseNotes = null,
            ),
        )
        every { gitHubTokenStore.getToken() } returns null

        val result = repository.checkForUpdates()
        assertTrue(result is Result.Success)
        coVerify(atLeast = 1) {
            gitHubReleasesClient.latestMatchingRelease(
                "owner/repo",
                any(),
                GitHubReleaseChannel.STABLE,
            )
        }
    }

    companion object {
        private const val PKG = "com.example.app"
    }
}

package dev.involvex.kiln.feature.apps

import app.cash.turbine.test
import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.core.model.ApkExportFormat
import dev.involvex.kiln.core.model.AppListFilter
import dev.involvex.kiln.core.model.AppUpdateInfo
import dev.involvex.kiln.core.model.AppUpdateRepository
import dev.involvex.kiln.core.model.InstalledAppDetail
import dev.involvex.kiln.core.model.InstalledAppSummary
import dev.involvex.kiln.core.model.ManagedSourceLink
import dev.involvex.kiln.core.model.MatchConfidence
import dev.involvex.kiln.core.model.RepoConfig
import dev.involvex.kiln.core.model.UpdateCheckResult
import dev.involvex.kiln.feature.apps.data.InstalledAppsRepository
import dev.involvex.kiln.feature.apps.export.ApkExporter
import dev.involvex.kiln.feature.apps.export.BatchExportScheduler
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppsViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `filters visible apps by query and chip`() = runTest {
        val repo = FakeRepository(
            listOf(
                summary("com.user", "User App", isSystem = false),
                summary("com.sys", "System App", isSystem = true),
            ),
        )
        val viewModel = AppsViewModel(repo, fakeScheduler(), ApkExporter(), FakeUpdates())

        viewModel.uiState.test {
            var state = awaitItem()
            while (state.visibleApps.isEmpty() && state.isLoading) {
                state = awaitItem()
            }
            assertEquals(1, state.visibleApps.size)
            assertEquals("com.user", state.visibleApps.first().packageName)

            viewModel.onEvent(AppsEvent.FilterChanged(AppListFilter.ALL))
            state = awaitItem()
            assertEquals(2, state.visibleApps.size)

            viewModel.onEvent(AppsEvent.QueryChanged("system"))
            state = awaitItem()
            assertEquals(listOf("com.sys"), state.visibleApps.map { it.packageName })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `updates filter shows only apps with badges`() = runTest {
        val repo = FakeRepository(
            listOf(
                summary("com.user", "User App", isSystem = false),
                summary("com.other", "Other", isSystem = false),
            ),
        )
        val updates = FakeUpdates(
            mapOf(
                "com.user" to AppUpdateInfo(
                    packageName = "com.user",
                    availableVersionName = "2.0",
                    availableVersionCode = 2,
                    sourceId = "fdroid",
                    sourceLabel = "F-Droid",
                    confidence = MatchConfidence.HIGH,
                    signatureMatches = true,
                ),
            ),
        )
        val viewModel = AppsViewModel(repo, fakeScheduler(), ApkExporter(), updates)
        advanceUntilIdle()

        viewModel.onEvent(AppsEvent.FilterChanged(AppListFilter.UPDATES))
        advanceUntilIdle()
        assertEquals(listOf("com.user"), viewModel.uiState.value.visibleApps.map { it.packageName })
        assertTrue(viewModel.uiState.value.updateBadges.containsKey("com.user"))
        assertFalse(viewModel.uiState.value.updateBadges["com.user"]!!.signatureWarning)
    }

    @Test
    fun `selection mode toggles and opens export dialog`() = runTest {
        val repo = FakeRepository(listOf(summary("com.user", "User App", isSystem = false)))
        val viewModel = AppsViewModel(repo, fakeScheduler(), ApkExporter(), FakeUpdates())
        advanceUntilIdle()

        viewModel.onEvent(AppsEvent.LongPressed("com.user"))
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.selectionMode)
        assertEquals(setOf("com.user"), viewModel.uiState.value.selectedPackages)

        viewModel.onEvent(AppsEvent.ExportSelection)
        advanceUntilIdle()
        assertEquals(listOf("com.user"), viewModel.uiState.value.exportDialog?.packageNames)
        assertFalse(viewModel.uiState.value.exportDialog?.isBatch == true)
        assertEquals(ApkExportFormat.APK, viewModel.uiState.value.exportDialog?.format)

        viewModel.onEvent(AppsEvent.SingleExportProgress(true))
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.singleExportProgress)
    }

    private fun fakeScheduler(): BatchExportScheduler {
        val scheduler = mockk<BatchExportScheduler>(relaxed = true)
        every { scheduler.observeWork() } returns emptyFlow()
        return scheduler
    }

    private fun summary(packageName: String, label: String, isSystem: Boolean) = InstalledAppSummary(
        packageName = packageName,
        label = label,
        versionName = "1.0",
        versionCode = 1,
        isSystem = isSystem,
        isUpdatedSystemApp = false,
        apkSizeBytes = 1000,
        hasSplits = false,
        lastUpdateTime = 1,
        firstInstallTime = 1,
    )

    private class FakeRepository(initial: List<InstalledAppSummary>) : InstalledAppsRepository {
        private val summariesState = MutableStateFlow(initial)
        override val summaries: StateFlow<List<InstalledAppSummary>> = summariesState.asStateFlow()
        private val loadingState = MutableStateFlow(false)
        override val isLoading: StateFlow<Boolean> = loadingState.asStateFlow()
        private val errorState = MutableStateFlow<String?>(null)
        override val lastError: StateFlow<String?> = errorState.asStateFlow()

        override fun refresh() = Unit

        override suspend fun detail(packageName: String): Result<InstalledAppDetail> = Result.failure("unused")

        override fun isInstalled(packageName: String): Boolean = true
    }

    private class FakeUpdates(private val initial: Map<String, AppUpdateInfo> = emptyMap()) : AppUpdateRepository {
        private val state = MutableStateFlow(initial)

        override fun updates(): Flow<Map<String, AppUpdateInfo>> = state.asStateFlow()

        override suspend fun refreshUpdates() = Unit

        override suspend fun checkForUpdates(): Result<UpdateCheckResult> = Result.success(UpdateCheckResult(1, 0, state.value.size))

        override fun observeIgnoreUpdates(packageName: String): Flow<Boolean?> = flowOf(false)

        override suspend fun setIgnoreUpdates(packageName: String, ignoreUpdates: Boolean): Result<Unit> = Result.success(Unit)

        override fun observeManagedLink(packageName: String): Flow<ManagedSourceLink?> = flowOf(null)

        override fun observeEnabledRepos(): Flow<List<RepoConfig>> = flowOf(emptyList())

        override suspend fun linkManagedSource(
            packageName: String,
            sourceId: String,
            confidence: MatchConfidence,
            githubOwnerRepo: String?,
            githubAssetRegex: String?,
            pinned: Boolean,
        ): Result<Unit> = Result.success(Unit)
    }
}

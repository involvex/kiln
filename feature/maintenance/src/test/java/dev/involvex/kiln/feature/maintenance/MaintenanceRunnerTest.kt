package dev.involvex.kiln.feature.maintenance

import android.content.Context
import dev.involvex.kiln.core.model.ApkDownloadJanitor
import dev.involvex.kiln.core.model.ApkRollbackCache
import dev.involvex.kiln.core.model.MaintenanceDefaults
import dev.involvex.kiln.core.model.MaintenancePrefs
import dev.involvex.kiln.core.model.PrivilegeStatus
import dev.involvex.kiln.core.model.PrivilegeTier
import dev.involvex.kiln.core.model.PrivilegedExecutor
import dev.involvex.kiln.core.model.RollbackApkEntry
import dev.involvex.kiln.core.model.ShizukuSetupPhase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class MaintenanceRunnerTest {
    @TempDir
    lateinit var temp: File

    @Test
    fun trim_skipped_without_clear_cache_capability() = runTest {
        val executor = mockk<PrivilegedExecutor>()
        every { executor.currentStatus() } returns PrivilegeStatus(
            phase = ShizukuSetupPhase.NOT_INSTALLED,
            tier = PrivilegeTier.NONE,
            capabilities = emptySet(),
            managerPackage = null,
            managerLabel = null,
            selfTest = null,
            binderAlive = false,
        )
        val downloads = mockk<ApkDownloadJanitor>()
        coEvery { downloads.pruneOlderThan(any(), any()) } returns 10L
        val rollback = FakeRollbackCache()
        val context = mockk<Context>()
        every { context.cacheDir } returns temp

        val runner = MaintenanceRunner(context, executor, downloads, rollback)
        val record = runner.run(
            MaintenancePrefs(
                enabled = true,
                trimCaches = true,
                pruneDownloads = true,
                pruneRollback = true,
                pruneBackupTemp = true,
            ),
            nowEpochMs = 100_000L,
        )
        val trim = record.tasks.first { it.taskId == MaintenanceDefaults.TASK_TRIM }
        assertEquals(MaintenanceDefaults.STATUS_SKIPPED, trim.status)
        assertEquals("skipped_no_privilege", trim.detail)
        assertTrue(record.ok)
    }
}

private class FakeRollbackCache : ApkRollbackCache {
    override fun observe(packageName: String): Flow<List<RollbackApkEntry>> = emptyFlow()

    override suspend fun list(packageName: String): List<RollbackApkEntry> = emptyList()

    override suspend fun rememberSuccessfulInstall(packageName: String, versionName: String, versionCode: Long, apkAbsolutePath: String) =
        Unit

    override suspend fun get(packageName: String, versionCode: Long): RollbackApkEntry? = null

    override suspend fun pruneByAgeAndSize(maxAgeMs: Long, maxTotalBytes: Long, nowEpochMs: Long): Long = 0L
}

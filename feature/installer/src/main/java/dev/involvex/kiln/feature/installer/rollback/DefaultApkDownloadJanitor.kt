package dev.involvex.kiln.feature.installer.rollback

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.involvex.kiln.core.common.IoDispatcher
import dev.involvex.kiln.core.model.ApkDownloadJanitor
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

@Singleton
class DefaultApkDownloadJanitor @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ApkDownloadJanitor {
    override suspend fun pruneOlderThan(maxAgeMs: Long, nowEpochMs: Long): Long = withContext(ioDispatcher) {
        val dir = File(context.cacheDir, DOWNLOAD_DIR)
        if (!dir.isDirectory) return@withContext 0L
        val cutoff = nowEpochMs - maxAgeMs
        var freed = 0L
        dir.listFiles()?.forEach { file ->
            if (file.isFile && file.lastModified() < cutoff) {
                val size = file.length()
                if (file.delete()) {
                    freed += size
                }
            }
        }
        freed
    }

    companion object {
        const val DOWNLOAD_DIR = "apk_downloads"
    }
}

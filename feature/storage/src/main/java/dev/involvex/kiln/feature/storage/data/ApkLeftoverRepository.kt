package dev.involvex.kiln.feature.storage.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.involvex.kiln.core.common.IoDispatcher
import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.core.model.ActivityLogEvents
import dev.involvex.kiln.core.model.ActivityLogRecorder
import dev.involvex.kiln.core.model.ActivityLogType
import dev.involvex.kiln.core.model.ApkLeftover
import dev.involvex.kiln.core.model.StoragePhase2Filter
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

@Singleton
class ApkLeftoverRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val activityLog: ActivityLogRecorder,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    suspend fun scanTree(treeUri: Uri): Result<List<ApkLeftover>> = withContext(ioDispatcher) {
        val root = DocumentFile.fromTreeUri(context, treeUri)
            ?: return@withContext Result.failure("invalid_tree")
        val found = ArrayList<ApkLeftover>()
        walk(root, found, depth = 0)
        Result.success(found.sortedByDescending { it.sizeBytes })
    }

    suspend fun delete(leftovers: List<ApkLeftover>): List<Pair<ApkLeftover, Boolean>> = withContext(ioDispatcher) {
        leftovers.map { item ->
            val ok = runCatching {
                val doc = DocumentFile.fromSingleUri(context, Uri.parse(item.uriString))
                doc != null && doc.exists() && doc.delete()
            }.getOrDefault(false)
            activityLog.record(
                ActivityLogEvents.create(
                    type = ActivityLogType.CACHE,
                    action = "apk_leftover_delete",
                    success = ok,
                    detailKey = item.displayName.take(DETAIL_NAME_MAX),
                ),
            )
            item to ok
        }
    }

    private fun walk(dir: DocumentFile, out: MutableList<ApkLeftover>, depth: Int) {
        if (depth > MAX_DEPTH || out.size >= MAX_FILES) return
        for (child in dir.listFiles()) {
            if (out.size >= MAX_FILES) return
            when {
                child.isDirectory -> walk(child, out, depth + 1)
                else -> maybeAddLeftover(child, out)
            }
        }
    }

    private fun maybeAddLeftover(file: DocumentFile, out: MutableList<ApkLeftover>) {
        val name = file.name ?: return
        if (!StoragePhase2Filter.isApkLeftoverName(name)) return
        val uri = file.uri.toString()
        out += ApkLeftover(
            id = uri,
            displayName = name,
            sizeBytes = file.length().coerceAtLeast(0L),
            uriString = uri,
        )
    }

    companion object {
        private const val MAX_DEPTH = 6
        private const val MAX_FILES = 500
        private const val DETAIL_NAME_MAX = 64
    }
}

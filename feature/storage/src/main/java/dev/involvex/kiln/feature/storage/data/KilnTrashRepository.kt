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
import dev.involvex.kiln.core.model.KilnTrashEntry
import dev.involvex.kiln.core.model.KilnTrashJanitor
import dev.involvex.kiln.core.model.StorageCleanerLogic
import dev.involvex.kiln.core.model.StorageTreePreferences
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

@Singleton
class KilnTrashRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val treePreferences: StorageTreePreferences,
    private val activityLog: ActivityLogRecorder,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : KilnTrashJanitor {
    suspend fun list(treeUri: Uri): Result<List<KilnTrashEntry>> = withContext(ioDispatcher) {
        val root = DocumentFile.fromTreeUri(context, treeUri)
            ?: return@withContext Result.failure("invalid_tree")
        Result.success(readManifest(root))
    }

    /**
     * Soft-delete: copy into `.kiln-trash/`, delete original, append manifest.
     * [items] are (displayName, sizeBytes, uriString, relativePathHint).
     */
    suspend fun moveToTrash(
        treeUri: Uri,
        items: List<TrashMoveRequest>,
        nowEpochMs: Long = System.currentTimeMillis(),
    ): List<Pair<TrashMoveRequest, Boolean>> = withContext(ioDispatcher) {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext items.map { it to false }
        val trashDir = root.findFile(StorageCleanerLogic.TRASH_DIR_NAME)?.takeIf { it.isDirectory }
            ?: root.createDirectory(StorageCleanerLogic.TRASH_DIR_NAME)
            ?: return@withContext items.map { it to false }
        val entries = readManifest(root).toMutableList()
        items.map { item ->
            val ok = runCatching { moveOne(trashDir, item, nowEpochMs, entries) }.getOrDefault(false)
            activityLog.record(
                ActivityLogEvents.create(
                    type = ActivityLogType.CACHE,
                    action = "kiln_trash_move",
                    success = ok,
                    detailKey = item.displayName.take(DETAIL_NAME_MAX),
                ),
            )
            item to ok
        }.also {
            writeManifest(trashDir, entries)
        }
    }

    suspend fun restore(treeUri: Uri, entries: List<KilnTrashEntry>): List<Pair<KilnTrashEntry, Boolean>> = withContext(ioDispatcher) {
        val root = DocumentFile.fromTreeUri(context, treeUri)
            ?: return@withContext entries.map { it to false }
        val trashDir = root.findFile(StorageCleanerLogic.TRASH_DIR_NAME)?.takeIf { it.isDirectory }
            ?: return@withContext entries.map { it to false }
        val manifest = readManifest(root).toMutableList()
        entries.map { entry ->
            val ok = runCatching { restoreOne(root, trashDir, entry) }.getOrDefault(false)
            if (ok) {
                manifest.removeAll { it.id == entry.id }
            }
            activityLog.record(
                ActivityLogEvents.create(
                    type = ActivityLogType.CACHE,
                    action = "kiln_trash_restore",
                    success = ok,
                    detailKey = entry.displayName.take(DETAIL_NAME_MAX),
                ),
            )
            entry to ok
        }.also {
            writeManifest(trashDir, manifest)
        }
    }

    suspend fun permanentlyDelete(treeUri: Uri, entries: List<KilnTrashEntry>): List<Pair<KilnTrashEntry, Boolean>> =
        withContext(ioDispatcher) {
            val root = DocumentFile.fromTreeUri(context, treeUri)
                ?: return@withContext entries.map { it to false }
            val trashDir = root.findFile(StorageCleanerLogic.TRASH_DIR_NAME)?.takeIf { it.isDirectory }
                ?: return@withContext entries.map { it to false }
            val manifest = readManifest(root).toMutableList()
            entries.map { entry ->
                val ok = runCatching {
                    val file = trashDir.findFile(entry.trashFileName)
                    file != null && file.exists() && file.delete()
                }.getOrDefault(false)
                if (ok) {
                    manifest.removeAll { it.id == entry.id }
                }
                activityLog.record(
                    ActivityLogEvents.create(
                        type = ActivityLogType.CACHE,
                        action = "kiln_trash_purge",
                        success = ok,
                        detailKey = entry.displayName.take(DETAIL_NAME_MAX),
                    ),
                )
                entry to ok
            }.also {
                writeManifest(trashDir, manifest)
            }
        }

    override suspend fun pruneOlderThan(maxAgeMs: Long, nowEpochMs: Long): Long = withContext(ioDispatcher) {
        val uriString = treePreferences.treeUri() ?: return@withContext 0L
        val treeUri = Uri.parse(uriString)
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext 0L
        val trashDir = root.findFile(StorageCleanerLogic.TRASH_DIR_NAME)?.takeIf { it.isDirectory }
            ?: return@withContext 0L
        val stale = readManifest(root).filter { nowEpochMs - it.movedAtEpochMs >= maxAgeMs }
        if (stale.isEmpty()) return@withContext 0L
        var freed = 0L
        val remaining = readManifest(root).toMutableList()
        stale.forEach { entry ->
            val file = trashDir.findFile(entry.trashFileName)
            if (file != null && file.exists() && file.delete()) {
                freed += entry.sizeBytes
                remaining.removeAll { it.id == entry.id }
            }
        }
        writeManifest(trashDir, remaining)
        freed
    }

    private fun moveOne(trashDir: DocumentFile, item: TrashMoveRequest, nowEpochMs: Long, entries: MutableList<KilnTrashEntry>): Boolean {
        val source = DocumentFile.fromSingleUri(context, Uri.parse(item.uriString))
        if (source == null || !source.exists() || !source.isFile) {
            return false
        }
        val trashName = "${UUID.randomUUID()}_${sanitizeFileName(item.displayName)}"
        val dest = trashDir.createFile(source.type ?: "application/octet-stream", trashName)
        if (dest == null || !copyDocument(source.uri, dest.uri)) {
            dest?.delete()
            return false
        }
        if (!source.delete()) {
            dest.delete()
            return false
        }
        val relative = item.relativePath.ifBlank { item.displayName }
        entries += KilnTrashEntry(
            id = dest.uri.toString(),
            originalRelativePath = relative,
            trashFileName = dest.name ?: trashName,
            displayName = item.displayName,
            sizeBytes = item.sizeBytes,
            movedAtEpochMs = nowEpochMs,
            uriString = dest.uri.toString(),
        )
        return true
    }

    private fun restoreOne(root: DocumentFile, trashDir: DocumentFile, entry: KilnTrashEntry): Boolean {
        val trashFile = trashDir.findFile(entry.trashFileName)
        if (trashFile == null || !trashFile.exists()) {
            return false
        }
        val target = ensureFile(root, entry.originalRelativePath, trashFile.type ?: "application/octet-stream")
        if (target == null || !copyDocument(trashFile.uri, target.uri)) {
            return false
        }
        return trashFile.delete()
    }

    private fun copyDocument(from: Uri, to: Uri): Boolean {
        val input = context.contentResolver.openInputStream(from) ?: return false
        val output = context.contentResolver.openOutputStream(to)
        if (output == null) {
            input.close()
            return false
        }
        return input.use { inStream ->
            output.use { outStream ->
                inStream.copyTo(outStream)
                true
            }
        }
    }

    private fun ensureFile(root: DocumentFile, relativePath: String, mime: String): DocumentFile? {
        val parts = relativePath.split('/').filter { it.isNotBlank() && it != "." && it != ".." }
        if (parts.isEmpty()) return null
        var dir = root
        for (segment in parts.dropLast(1)) {
            val next = dir.findFile(segment)?.takeIf { it.isDirectory }
                ?: dir.createDirectory(segment)
                ?: return null
            dir = next
        }
        val fileName = parts.last()
        dir.findFile(fileName)?.delete()
        return dir.createFile(mime, fileName)
    }

    private fun readManifest(root: DocumentFile): List<KilnTrashEntry> {
        val trashDir = root.findFile(StorageCleanerLogic.TRASH_DIR_NAME)?.takeIf { it.isDirectory }
            ?: return emptyList()
        val manifestFile = trashDir.findFile(StorageCleanerLogic.TRASH_MANIFEST_NAME) ?: return emptyList()
        val text = context.contentResolver.openInputStream(manifestFile.uri)?.bufferedReader()?.use { it.readText() }
            ?: return emptyList()
        return KilnTrashManifest.decode(text) { name ->
            trashDir.findFile(name)?.uri?.toString().orEmpty()
        }.filter { it.uriString.isNotBlank() }
    }

    private fun writeManifest(trashDir: DocumentFile, entries: List<KilnTrashEntry>) {
        val existing = trashDir.findFile(StorageCleanerLogic.TRASH_MANIFEST_NAME)
        existing?.delete()
        if (entries.isEmpty()) return
        val file = trashDir.createFile("text/tab-separated-values", StorageCleanerLogic.TRASH_MANIFEST_NAME)
            ?: return
        context.contentResolver.openOutputStream(file.uri)?.bufferedWriter()?.use { writer ->
            writer.write(KilnTrashManifest.encode(entries))
        }
    }

    private fun sanitizeFileName(name: String): String {
        val cleaned = name.replace(Regex("[\\\\/:*?\"<>|]"), "_").take(80)
        return cleaned.ifBlank { "file" }
    }

    companion object {
        private const val DETAIL_NAME_MAX = 64

        fun defaultMaxAgeMs(): Long = TimeUnit.DAYS.toMillis(StorageCleanerLogic.TRASH_MAX_AGE_DAYS)
    }
}

data class TrashMoveRequest(val displayName: String, val sizeBytes: Long, val uriString: String, val relativePath: String)

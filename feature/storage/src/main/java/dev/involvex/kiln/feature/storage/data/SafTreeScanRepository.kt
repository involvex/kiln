package dev.involvex.kiln.feature.storage.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.involvex.kiln.core.common.IoDispatcher
import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.core.model.ApkLeftover
import dev.involvex.kiln.core.model.DuplicateFileGroup
import dev.involvex.kiln.core.model.SafTreeFile
import dev.involvex.kiln.core.model.StorageCleanerLogic
import dev.involvex.kiln.core.model.StoragePhase2Filter
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

data class SafTreeScanResult(val leftovers: List<ApkLeftover>, val largeFiles: List<SafTreeFile>, val duplicates: List<DuplicateFileGroup>)

@Singleton
class SafTreeScanRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    suspend fun scanTree(treeUri: Uri): Result<SafTreeScanResult> = withContext(ioDispatcher) {
        val root = DocumentFile.fromTreeUri(context, treeUri)
            ?: return@withContext Result.failure("invalid_tree")
        val files = ArrayList<SafTreeFile>()
        walk(root, relativePrefix = "", out = files, depth = 0)
        val leftovers = files
            .asSequence()
            .filter { StoragePhase2Filter.isApkLeftoverName(it.displayName) }
            .map {
                ApkLeftover(
                    id = it.id,
                    displayName = it.displayName,
                    sizeBytes = it.sizeBytes,
                    uriString = it.uriString,
                    relativePath = it.relativePath,
                )
            }
            .sortedByDescending { it.sizeBytes }
            .toList()
        val largeFiles = StorageCleanerLogic.largeFiles(files)
        val sameSize = StorageCleanerLogic.sameSizeGroups(files)
        val candidates = StorageCleanerLogic.hashCandidates(sameSize)
        val hashed = candidates.mapNotNull { file ->
            val digest = sha256(Uri.parse(file.uriString)) ?: return@mapNotNull null
            file to digest
        }
        val duplicates = StorageCleanerLogic.duplicateGroupsFromHashes(hashed)
        Result.success(
            SafTreeScanResult(
                leftovers = leftovers,
                largeFiles = largeFiles,
                duplicates = duplicates,
            ),
        )
    }

    private fun walk(dir: DocumentFile, relativePrefix: String, out: MutableList<SafTreeFile>, depth: Int) {
        if (depth > MAX_DEPTH || out.size >= MAX_FILES) return
        dir.listFiles()
            .asSequence()
            .takeWhile { out.size < MAX_FILES }
            .forEach { child ->
                val name = child.name ?: return@forEach
                if (name == StorageCleanerLogic.TRASH_DIR_NAME) return@forEach
                val relative = if (relativePrefix.isEmpty()) name else "$relativePrefix/$name"
                when {
                    child.isDirectory -> walk(child, relative, out, depth + 1)

                    child.isFile -> {
                        out += SafTreeFile(
                            id = child.uri.toString(),
                            displayName = name,
                            sizeBytes = child.length().coerceAtLeast(0L),
                            uriString = child.uri.toString(),
                            relativePath = relative,
                        )
                    }
                }
            }
    }

    private fun sha256(uri: Uri): String? = runCatching {
        val digest = MessageDigest.getInstance("SHA-256")
        context.contentResolver.openInputStream(uri)?.use { input ->
            val buffer = ByteArray(BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        } ?: return null
        digest.digest().joinToString("") { byte -> "%02x".format(byte) }
    }.getOrNull()

    companion object {
        private const val MAX_DEPTH = 6
        private const val MAX_FILES = 2_000
        private const val BUFFER_SIZE = 64 * 1024
    }
}

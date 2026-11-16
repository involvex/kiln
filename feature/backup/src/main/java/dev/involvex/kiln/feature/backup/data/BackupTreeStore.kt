package dev.involvex.kiln.feature.backup.data

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.involvex.kiln.core.model.BackupPrefs
import dev.involvex.kiln.core.model.BackupRotation
import dev.involvex.kiln.core.model.BackupTreeEntry
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupTreeStore @Inject constructor(@ApplicationContext private val context: Context) {
    fun treeRoot(treeUri: String?): DocumentFile? {
        if (treeUri.isNullOrBlank()) return null
        return DocumentFile.fromTreeUri(context, Uri.parse(treeUri))
    }

    fun displayLabel(treeUri: String?): String? {
        val root = treeRoot(treeUri) ?: return null
        return root.name ?: treeUri
    }

    fun createBackupFile(treeUri: String, packageName: String, fileName: String): DocumentFile? {
        val root = treeRoot(treeUri) ?: return null
        if (!root.canWrite()) return null
        val dir = root.findFile(packageName)?.takeIf { it.isDirectory }
            ?: root.createDirectory(packageName)
            ?: return null
        dir.findFile(fileName)?.delete()
        return dir.createFile("application/zip", fileName)
    }

    fun listPackageArchives(treeUri: String, packageName: String): List<BackupTreeEntry> {
        val root = treeRoot(treeUri) ?: return emptyList()
        val dir = root.findFile(packageName)?.takeIf { it.isDirectory } ?: return emptyList()
        return dir.listFiles()
            .filter {
                it.isFile && (
                    it.name?.endsWith(".zip", ignoreCase = true) == true ||
                        it.name?.endsWith(".kilnbackup.zip", ignoreCase = true) == true ||
                        it.name?.contains("kilnbackup", ignoreCase = true) == true
                    )
            }
            .mapNotNull { file ->
                val name = file.name ?: return@mapNotNull null
                val uri = file.uri.toString()
                BackupTreeEntry(
                    displayName = name,
                    uriString = uri,
                    lastModifiedEpochMs = file.lastModified(),
                )
            }
    }

    fun prunePackageArchives(treeUri: String, packageName: String, keepN: Int): Int {
        val toDelete = BackupRotation.selectForDeletion(listPackageArchives(treeUri, packageName), keepN)
        var deleted = 0
        for (entry in toDelete) {
            val doc = DocumentFile.fromSingleUri(context, Uri.parse(entry.uriString))
            if (doc?.delete() == true) deleted++
        }
        return deleted
    }

    fun takePersistablePermission(uri: Uri, contentResolver: ContentResolver) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        contentResolver.takePersistableUriPermission(uri, flags)
    }

    fun releasePersistablePermission(uriString: String?, contentResolver: ContentResolver) {
        if (uriString.isNullOrBlank()) return
        val uri = Uri.parse(uriString)
        try {
            contentResolver.releasePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        } catch (_: SecurityException) {
            // Already revoked or never granted.
        }
    }

    fun suggestedKeepN(): Int = BackupPrefs.DEFAULT_KEEP_N
}

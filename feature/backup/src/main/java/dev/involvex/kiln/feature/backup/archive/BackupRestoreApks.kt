package dev.involvex.kiln.feature.backup.archive

import java.io.File

/**
 * Pure helpers for restore install paths (unit-tested).
 * Base APK first, then remaining split APKs — matches [LocalInstallRequest] ordering.
 */
object BackupRestoreApks {
    /**
     * Ordered absolute paths for [LocalInstallRequest]: base first, then other APKs by name.
     */
    fun orderedAbsolutePaths(baseApk: File, allApkFiles: List<File>): List<String> {
        val rest = allApkFiles
            .asSequence()
            .filter { it.absolutePath != baseApk.absolutePath }
            .sortedBy { it.name.lowercase() }
            .map { it.absolutePath }
            .toList()
        return listOf(baseApk.absolutePath) + rest
    }

    /**
     * True when metadata claims splits but the archive only contained a single APK.
     */
    fun isIncompleteSplitArchive(hasSplits: Boolean, apkCount: Int): Boolean = hasSplits && apkCount < 2
}

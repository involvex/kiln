package dev.involvex.kiln.feature.installer.rollback

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.involvex.kiln.core.model.ApkRollbackCache
import dev.involvex.kiln.core.model.RollbackApkEntry
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

/**
 * Keeps up to [MAX_PER_PACKAGE] successfully installed APKs per package under app filesDir.
 */
@Singleton
class FileApkRollbackCache @Inject constructor(@ApplicationContext private val context: Context) : ApkRollbackCache {
    private val root = File(context.filesDir, ROOT_DIR).also { it.mkdirs() }
    private val mutex = Mutex()
    private val snapshot = MutableStateFlow(scanAll())

    override fun observe(packageName: String): Flow<List<RollbackApkEntry>> =
        snapshot.map { all -> all.filter { it.packageName == packageName }.sortedByDescending { it.versionCode } }

    override suspend fun list(packageName: String): List<RollbackApkEntry> = mutex.withLock {
        entriesFor(packageName).sortedByDescending { it.versionCode }
    }

    override suspend fun rememberSuccessfulInstall(packageName: String, versionName: String, versionCode: Long, apkAbsolutePath: String) =
        mutex.withLock {
            val source = File(apkAbsolutePath)
            if (!source.isFile) {
                Timber.w("Rollback cache skip: missing apk for %s", packageName)
                return@withLock
            }
            val pkgDir = File(root, sanitizePackage(packageName)).also { it.mkdirs() }
            val dest = File(pkgDir, "$versionCode.apk")
            val meta = File(pkgDir, "$versionCode.meta")
            try {
                source.copyTo(dest, overwrite = true)
                meta.writeText(
                    listOf(
                        versionName.replace('\n', ' ').replace('|', '/'),
                        versionCode.toString(),
                        System.currentTimeMillis().toString(),
                        dest.length().toString(),
                    ).joinToString("|"),
                )
            } catch (e: IOException) {
                Timber.w(e, "Rollback cache copy failed for %s", packageName)
                dest.delete()
                meta.delete()
                return@withLock
            }
            prunePackageLocked(pkgDir, keep = MAX_PER_PACKAGE)
            snapshot.value = scanAll()
        }

    override suspend fun get(packageName: String, versionCode: Long): RollbackApkEntry? = mutex.withLock {
        entriesFor(packageName).firstOrNull { it.versionCode == versionCode }
    }

    private fun entriesFor(packageName: String): List<RollbackApkEntry> {
        val pkgDir = File(root, sanitizePackage(packageName))
        if (!pkgDir.isDirectory) return emptyList()
        return pkgDir.listFiles()
            ?.filter { it.extension == "apk" }
            ?.mapNotNull { apk -> readEntry(packageName, apk) }
            .orEmpty()
    }

    private fun readEntry(packageName: String, apk: File): RollbackApkEntry? {
        val code = apk.nameWithoutExtension.toLongOrNull() ?: return null
        val meta = File(apk.parentFile, "$code.meta")
        val parts = if (meta.isFile) meta.readText().split('|') else emptyList()
        val versionName = parts.getOrNull(0)?.ifBlank { null } ?: code.toString()
        val versionCode = parts.getOrNull(1)?.toLongOrNull() ?: code
        val savedAt = parts.getOrNull(2)?.toLongOrNull() ?: apk.lastModified()
        val size = parts.getOrNull(3)?.toLongOrNull() ?: apk.length()
        if (!apk.isFile) return null
        return RollbackApkEntry(
            packageName = packageName,
            versionName = versionName,
            versionCode = versionCode,
            apkAbsolutePath = apk.absolutePath,
            savedAtEpochMs = savedAt,
            sizeBytes = size,
        )
    }

    private fun prunePackageLocked(pkgDir: File, keep: Int) {
        val apks = pkgDir.listFiles()?.filter { it.extension == "apk" }.orEmpty()
            .sortedByDescending { it.nameWithoutExtension.toLongOrNull() ?: 0L }
        apks.drop(keep).forEach { apk ->
            val code = apk.nameWithoutExtension
            apk.delete()
            File(pkgDir, "$code.meta").delete()
        }
    }

    private fun scanAll(): List<RollbackApkEntry> {
        if (!root.isDirectory) return emptyList()
        return root.listFiles()
            ?.filter { it.isDirectory }
            ?.flatMap { dir ->
                val pkg = dir.name
                dir.listFiles()
                    ?.filter { it.extension == "apk" }
                    ?.mapNotNull { apk -> readEntry(pkg, apk) }
                    .orEmpty()
            }
            .orEmpty()
    }

    private fun sanitizePackage(packageName: String): String = packageName.filter { it.isLetterOrDigit() || it == '.' || it == '_' }

    private companion object {
        const val ROOT_DIR = "apk_rollback"
        const val MAX_PER_PACKAGE = 3
    }
}

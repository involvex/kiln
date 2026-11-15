package dev.involvex.kiln.feature.apps.apktools

import com.reandroid.apk.ApkBundle
import com.reandroid.apk.ApkModule
import com.reandroid.archive.ZipEntryMap
import java.io.File
import java.io.IOException
import java.util.regex.Pattern

/**
 * Merges split APK modules from an `.apks` / `.xapk` / `.apkm` bundle into one APK
 * using REAndroid ARSCLib ([ApkBundle]), then clears META-INF for resign.
 *
 * Merge / META-INF clearing behaviour follows Apache-2.0 REAndroid APKEditor
 * (`Merger` / `CommandExecutor.clearMeta`). SPDX: Apache-2.0.
 */
object UniversalApkMerger {
    private val MetaInfSignaturePattern: Pattern =
        Pattern.compile("^META-INF/.+\\.(([MS]F)|(RSA))")

    private val KnownErrorCodes = setOf(
        "bundle_missing",
        "dest_unwritable",
        "bundle_no_apk",
        "bundle_extract_failed",
        "merge_protected",
        "merge_failed",
        "merge_oom",
    )

    fun isMergeableKind(kind: dev.involvex.kiln.core.model.ApkArchiveKind): Boolean =
        kind == dev.involvex.kiln.core.model.ApkArchiveKind.APKS ||
            kind == dev.involvex.kiln.core.model.ApkArchiveKind.XAPK ||
            kind == dev.involvex.kiln.core.model.ApkArchiveKind.APKM

    /**
     * Extracts member APKs from [bundleFile], merges them into [outputApk], and
     * strips signature entries so [ApkResign] can sign the result.
     */
    fun mergeBundleToApk(bundleFile: File, workDir: File, outputApk: File): kotlin.Result<Unit> = try {
        runMerge(bundleFile, workDir, outputApk)
    } catch (oom: OutOfMemoryError) {
        kotlin.Result.failure(IllegalStateException("merge_oom", oom))
    }

    private fun runMerge(bundleFile: File, workDir: File, outputApk: File): kotlin.Result<Unit> = runCatching {
        require(bundleFile.isFile) { "bundle_missing" }
        require(workDir.isDirectory || workDir.mkdirs()) { "dest_unwritable" }
        if (outputApk.exists()) {
            outputApk.delete()
        }
        outputApk.parentFile?.mkdirs()

        val splitsDir = File(workDir, "splits").apply {
            deleteRecursively()
            mkdirs()
        }
        extractSplitsOrThrow(bundleFile, splitsDir)
        mergeLoadedSplits(splitsDir, outputApk)
        require(outputApk.isFile && outputApk.length() > 0L) { "merge_failed" }
    }.fold(
        onSuccess = { kotlin.Result.success(Unit) },
        onFailure = { error ->
            kotlin.Result.failure(IllegalStateException(mapMergeError(error), error))
        },
    )

    private fun extractSplitsOrThrow(bundleFile: File, splitsDir: File) {
        val extracted = ApkSplitExporter.extractApkMembers(bundleFile, splitsDir)
        if (extracted.isFailure) {
            error(extracted.exceptionOrNull()?.message ?: "bundle_extract_failed")
        }
        require(extracted.getOrThrow() >= 1) { "bundle_no_apk" }
    }

    @Throws(IOException::class)
    private fun mergeLoadedSplits(splitsDir: File, outputApk: File) {
        ApkBundle().use { bundle ->
            bundle.loadApkDirectory(splitsDir, false)
            for (module in bundle.apkModuleList) {
                if (isProtected(module)) {
                    error("merge_protected")
                }
            }
            // validateModules=true → ApkBundle.mergeModules(force=true)
            val merged = bundle.mergeModules(true)
            clearMeta(merged)
            merged.refreshTable()
            merged.refreshManifest()
            merged.writeApk(outputApk)
            merged.close()
        }
    }

    private fun mapMergeError(error: Throwable): String {
        val msg = error.message
        return if (msg != null && msg in KnownErrorCodes) msg else "merge_failed"
    }

    /**
     * Detects APKEditor-style “protected” resource layouts that block safe merge.
     * Adapted from REAndroid APKEditor `Util.isProtected` (Apache-2.0).
     */
    internal fun isProtected(module: ApkModule): Boolean {
        val zip: ZipEntryMap = module.zipEntryMap
        val iterator = zip.iteratorWithPath { path ->
            path != null && (path.startsWith("classes.dex/") || path.startsWith("AndroidManifest.xml/"))
        }
        return iterator.hasNext()
    }

    /**
     * Removes signature block + META-INF signature files (APKEditor `clearMeta`).
     */
    internal fun clearMeta(module: ApkModule) {
        val archive = module.zipEntryMap
        archive.removeIf(MetaInfSignaturePattern)
        archive.remove("stamp-cert-sha256")
        module.apkSignatureBlock = null
    }
}

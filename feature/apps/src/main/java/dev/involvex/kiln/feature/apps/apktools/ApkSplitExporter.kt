package dev.involvex.kiln.feature.apps.apktools

import java.io.File
import java.util.zip.ZipFile

/**
 * Exports split APK members from `.apks` / `.xapk` zip bundles (Tier NONE).
 * Does not merge into a universal APK.
 */
object ApkSplitExporter {
    fun apkMemberNames(bundle: File): List<String> =
        ApkArchiveReader.listZipMembers(bundle).filter { it.endsWith(".apk", ignoreCase = true) }

    fun extractApkMembers(bundle: File, destinationDir: File): kotlin.Result<Int> = runCatching {
        require(bundle.isFile) { "bundle_missing" }
        require(destinationDir.isDirectory || destinationDir.mkdirs()) { "dest_unwritable" }
        val members = apkMemberNames(bundle)
        require(members.isNotEmpty()) { "bundle_no_apk" }
        var written = 0
        ZipFile(bundle).use { zip ->
            members.forEach { name ->
                val entry = zip.getEntry(name) ?: return@forEach
                val safeName = File(name).name.replace(Regex("[^A-Za-z0-9._-]"), "_")
                    .ifBlank { "split-$written.apk" }
                val out = File(destinationDir, safeName)
                zip.getInputStream(entry).use { input ->
                    out.outputStream().use { output -> input.copyTo(output) }
                }
                written += 1
            }
        }
        require(written > 0) { "bundle_extract_failed" }
        written
    }.fold(
        onSuccess = { kotlin.Result.success(it) },
        onFailure = {
            val code = when (val msg = it.message) {
                "bundle_missing", "dest_unwritable", "bundle_no_apk", "bundle_extract_failed" -> msg
                else -> "bundle_extract_failed"
            }
            kotlin.Result.failure(IllegalStateException(code))
        },
    )
}

package dev.involvex.kiln.feature.apps.data

import dev.involvex.kiln.core.model.InstalledAppDetail
import dev.involvex.kiln.core.model.InstalledAppSummary

/**
 * Android PackageManager access boundary. Features depend on this interface only.
 */
interface PackageInfoSource {
    suspend fun getInstalledSummaries(): List<InstalledAppSummary>

    suspend fun getDetail(packageName: String): InstalledAppDetail?

    fun isPackageInstalled(packageName: String): Boolean
}

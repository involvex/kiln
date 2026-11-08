package dev.involvex.kiln.feature.apps.data

import dev.involvex.kiln.core.common.IoDispatcher
import dev.involvex.kiln.core.model.InstalledAppMatchInput
import dev.involvex.kiln.core.model.InstalledAppsReader
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

@Singleton
class DefaultInstalledAppsReader @Inject constructor(
    private val packageInfoSource: PackageInfoSource,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : InstalledAppsReader {
    override suspend fun userApps(): List<InstalledAppMatchInput> = withContext(ioDispatcher) {
        packageInfoSource.getInstalledSummaries()
            .filter { !it.isSystem }
            .map { summary ->
                val detail = packageInfoSource.getDetail(summary.packageName)
                InstalledAppMatchInput(
                    packageName = summary.packageName,
                    label = summary.label,
                    versionName = summary.versionName,
                    versionCode = summary.versionCode,
                    signerSha256 = detail?.signerSha256.orEmpty(),
                )
            }
    }
}

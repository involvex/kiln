package dev.involvex.kiln.feature.apps.activities

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.involvex.kiln.core.common.IoDispatcher
import dev.involvex.kiln.core.model.AppActivityInfo
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

@Singleton
class AppActivitiesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val packageManager: PackageManager = context.packageManager

    suspend fun exportedActivities(packageName: String): List<AppActivityInfo> = withContext(ioDispatcher) {
        val info = packageInfoOrNull(packageName) ?: return@withContext emptyList()
        val activities = info.activities ?: return@withContext emptyList()
        activities.mapNotNull { activity ->
            // Manifest `android:exported` (and defaults) are reflected on ActivityInfo.exported.
            if (!activity.exported) return@mapNotNull null
            val label = activity.loadLabel(packageManager)?.toString()?.takeIf { it.isNotBlank() }
                ?: activity.name.substringAfterLast('.')
            AppActivityInfo(
                packageName = packageName,
                className = activity.name,
                label = label,
                exported = true,
                launchMode = activity.launchMode,
                taskAffinity = activity.taskAffinity?.takeIf { it.isNotBlank() },
                activityFlags = activity.flags,
            )
        }.sortedBy { it.label.lowercase() }
    }

    private fun packageInfoOrNull(packageName: String) = try {
        // GET_ACTIVITIES needed for components; API 33+ uses PackageInfoFlags.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(
                packageName,
                PackageManager.PackageInfoFlags.of(PackageManager.GET_ACTIVITIES.toLong()),
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
        }
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }
}

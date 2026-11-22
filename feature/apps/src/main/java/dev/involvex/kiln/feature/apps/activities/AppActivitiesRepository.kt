package dev.involvex.kiln.feature.apps.activities

import android.content.Context
import android.content.pm.ComponentInfo
import android.content.pm.PackageManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.involvex.kiln.core.common.IoDispatcher
import dev.involvex.kiln.core.model.AppActivityInfo
import dev.involvex.kiln.core.model.AppComponentFilter
import dev.involvex.kiln.core.model.AppComponentInfo
import dev.involvex.kiln.core.model.AppComponentKind
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

data class ExportedComponents(
    val activities: List<AppActivityInfo>,
    val services: List<AppComponentInfo>,
    val receivers: List<AppComponentInfo>,
    val providers: List<AppComponentInfo>,
)

@Singleton
class AppActivitiesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val packageManager: PackageManager = context.packageManager

    suspend fun exportedActivities(packageName: String): List<AppActivityInfo> = exportedComponents(packageName).activities

    suspend fun exportedComponents(packageName: String): ExportedComponents = withContext(ioDispatcher) {
        val info = packageInfoOrNull(packageName)
            ?: return@withContext ExportedComponents(emptyList(), emptyList(), emptyList(), emptyList())
        val activities = AppComponentFilter.exportedOnly(info.activities?.toList().orEmpty()) { it.exported }
            .map { activity ->
                AppActivityInfo(
                    packageName = packageName,
                    className = activity.name,
                    label = componentLabel(activity),
                    exported = true,
                    launchMode = activity.launchMode,
                    taskAffinity = activity.taskAffinity?.takeIf { it.isNotBlank() },
                    activityFlags = activity.flags,
                )
            }
            .sortedBy { it.label.lowercase() }
        ExportedComponents(
            activities = activities,
            services = mapExported(packageName, info.services?.toList().orEmpty(), AppComponentKind.SERVICE),
            receivers = mapExported(packageName, info.receivers?.toList().orEmpty(), AppComponentKind.RECEIVER),
            providers = mapExported(packageName, info.providers?.toList().orEmpty(), AppComponentKind.PROVIDER),
        )
    }

    private fun mapExported(packageName: String, items: List<ComponentInfo>, kind: AppComponentKind): List<AppComponentInfo> =
        AppComponentFilter.exportedOnly(items) { it.exported }
            .map { component ->
                AppComponentInfo(
                    packageName = packageName,
                    className = component.name,
                    label = componentLabel(component),
                    exported = true,
                    kind = kind,
                )
            }
            .sortedBy { it.label.lowercase() }

    private fun componentLabel(component: ComponentInfo): String =
        component.loadLabel(packageManager)?.toString()?.takeIf { it.isNotBlank() }
            ?: component.name.substringAfterLast('.')

    private fun packageInfoOrNull(packageName: String) = try {
        // GET_* needed for component arrays; API 33+ uses PackageInfoFlags.
        val flags = PackageManager.GET_ACTIVITIES or
            PackageManager.GET_SERVICES or
            PackageManager.GET_RECEIVERS or
            PackageManager.GET_PROVIDERS
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(
                packageName,
                PackageManager.PackageInfoFlags.of(flags.toLong()),
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, flags)
        }
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }
}

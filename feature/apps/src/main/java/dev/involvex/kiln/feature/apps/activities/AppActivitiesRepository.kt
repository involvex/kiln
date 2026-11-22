package dev.involvex.kiln.feature.apps.activities

import android.content.ComponentName
import android.content.Context
import android.content.pm.ComponentInfo
import android.content.pm.PackageManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.involvex.kiln.core.common.IoDispatcher
import dev.involvex.kiln.core.model.AppActivityInfo
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
        val activities = info.activities?.toList().orEmpty()
            .filter { it.exported }
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
            services = mapComponents(packageName, info.services?.toList().orEmpty(), AppComponentKind.SERVICE),
            receivers = mapComponents(packageName, info.receivers?.toList().orEmpty(), AppComponentKind.RECEIVER),
            providers = mapComponents(packageName, info.providers?.toList().orEmpty(), AppComponentKind.PROVIDER),
        )
    }

    private fun mapComponents(packageName: String, items: List<ComponentInfo>, kind: AppComponentKind): List<AppComponentInfo> = items
        .map { component ->
            AppComponentInfo(
                packageName = packageName,
                className = component.name,
                label = componentLabel(component),
                exported = component.exported,
                enabled = isComponentEnabled(packageName, component),
                kind = kind,
            )
        }
        .sortedWith(compareByDescending<AppComponentInfo> { it.exported }.thenBy { it.label.lowercase() })

    private fun isComponentEnabled(packageName: String, component: ComponentInfo): Boolean {
        val state = packageManager.getComponentEnabledSetting(ComponentName(packageName, component.name))
        return when (state) {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED,
            -> false

            PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true

            else -> component.enabled
        }
    }

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

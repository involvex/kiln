package dev.involvex.kiln.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import dev.involvex.kiln.R
import dev.involvex.kiln.feature.apps.AppDetailRoute
import dev.involvex.kiln.feature.apps.AppsRoute
import dev.involvex.kiln.feature.backup.BackupRoute
import dev.involvex.kiln.feature.debloat.DebloatRoute
import dev.involvex.kiln.feature.onboarding.AutoMatchRoute
import dev.involvex.kiln.feature.onboarding.privilege.PrivilegeSetupRoute
import dev.involvex.kiln.feature.settings.SettingsRoute
import dev.involvex.kiln.feature.sources.SourcesRoute
import dev.involvex.kiln.feature.storage.StorageRoute
import dev.involvex.kiln.feature.termux.TermuxRoute
import dev.involvex.kiln.navigation.AppDetailKey
import dev.involvex.kiln.navigation.AppsKey
import dev.involvex.kiln.navigation.AutoMatchKey
import dev.involvex.kiln.navigation.BackupKey
import dev.involvex.kiln.navigation.DebloatKey
import dev.involvex.kiln.navigation.PrivilegeSetupKey
import dev.involvex.kiln.navigation.SettingsKey
import dev.involvex.kiln.navigation.SourcesKey
import dev.involvex.kiln.navigation.StorageKey
import dev.involvex.kiln.navigation.TermuxKey
import dev.involvex.kiln.navigation.TopLevelBackStack

private data class TopLevelDestination(val key: Any, val labelRes: Int, val icon: ImageVector)

@Composable
fun KilnApp(modifier: Modifier = Modifier) {
    val destinations = listOf(
        TopLevelDestination(AppsKey, R.string.nav_apps, Icons.Filled.Apps),
        TopLevelDestination(SourcesKey, R.string.nav_sources, Icons.Filled.CloudDownload),
        TopLevelDestination(StorageKey, R.string.nav_storage, Icons.Filled.Storage),
        TopLevelDestination(BackupKey, R.string.nav_backup, Icons.Filled.Save),
        TopLevelDestination(SettingsKey, R.string.nav_settings, Icons.Filled.Settings),
    )
    val topLevelBackStack = remember { TopLevelBackStack<Any>(AppsKey) }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar {
                destinations.forEach { destination ->
                    val selected = destination.key == topLevelBackStack.topLevelKey
                    NavigationBarItem(
                        selected = selected,
                        onClick = { topLevelBackStack.addTopLevel(destination.key) },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = stringResource(destination.labelRes),
                            )
                        },
                        label = { Text(text = stringResource(destination.labelRes)) },
                    )
                }
            }
        },
    ) { padding ->
        NavDisplay(
            backStack = topLevelBackStack.backStack,
            onBack = { topLevelBackStack.removeLast() },
            modifier = Modifier.padding(padding),
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = entryProvider {
                kilnNavEntries(topLevelBackStack)
            },
        )
    }
}

private fun EntryProviderScope<Any>.kilnNavEntries(topLevelBackStack: TopLevelBackStack<Any>) {
    entry<AppsKey> {
        AppsRoute(
            onOpenDetail = { packageName ->
                topLevelBackStack.add(AppDetailKey(packageName))
            },
        )
    }
    entry<AppDetailKey> { key ->
        AppDetailRoute(
            packageName = key.packageName,
            onBack = { topLevelBackStack.removeLast() },
        )
    }
    entry<SourcesKey> {
        SourcesRoute(
            onOpenAutoMatch = { topLevelBackStack.add(AutoMatchKey) },
        )
    }
    entry<AutoMatchKey> {
        AutoMatchRoute(onBack = { topLevelBackStack.removeLast() })
    }
    entry<StorageKey> { StorageRoute() }
    entry<BackupKey> { BackupRoute() }
    entry<SettingsKey> {
        SettingsRoute(
            onOpenPrivilegeSetup = { topLevelBackStack.add(PrivilegeSetupKey) },
            onOpenDebloat = { topLevelBackStack.add(DebloatKey) },
            onOpenTermux = { topLevelBackStack.add(TermuxKey) },
        )
    }
    entry<PrivilegeSetupKey> {
        PrivilegeSetupRoute(onBack = { topLevelBackStack.removeLast() })
    }
    entry<DebloatKey> {
        DebloatRoute(
            onBack = { topLevelBackStack.removeLast() },
            onOpenPrivilegeSetup = { topLevelBackStack.add(PrivilegeSetupKey) },
        )
    }
    entry<TermuxKey> {
        TermuxRoute(onBack = { topLevelBackStack.removeLast() })
    }
}

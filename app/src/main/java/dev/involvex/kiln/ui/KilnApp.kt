package dev.involvex.kiln.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import dev.involvex.kiln.R
import dev.involvex.kiln.core.designsystem.scroll.LocalScrollToTopSignal
import dev.involvex.kiln.core.designsystem.scroll.ScrollToTopSignal
import dev.involvex.kiln.feature.apps.AppDetailRoute
import dev.involvex.kiln.feature.debloat.DebloatRoute
import dev.involvex.kiln.feature.onboarding.AutoMatchRoute
import dev.involvex.kiln.feature.onboarding.privilege.PrivilegeSetupRoute
import dev.involvex.kiln.feature.settings.SettingsRoute
import dev.involvex.kiln.feature.settings.activity.ActivityLogRoute
import dev.involvex.kiln.feature.sources.SourcesRoute
import dev.involvex.kiln.feature.sources.discovery.CatalogDetailRoute
import dev.involvex.kiln.feature.sources.discovery.DiscoveryRoute
import dev.involvex.kiln.feature.termux.TermuxRoute
import dev.involvex.kiln.feature.widgets.ManageDeepLinkStore
import dev.involvex.kiln.navigation.ActivityLogKey
import dev.involvex.kiln.navigation.AppDetailKey
import dev.involvex.kiln.navigation.AppsKey
import dev.involvex.kiln.navigation.AutoMatchKey
import dev.involvex.kiln.navigation.CatalogDetailKey
import dev.involvex.kiln.navigation.DebloatKey
import dev.involvex.kiln.navigation.DiscoveryKey
import dev.involvex.kiln.navigation.ManageKey
import dev.involvex.kiln.navigation.PrivilegeSetupKey
import dev.involvex.kiln.navigation.SettingsKey
import dev.involvex.kiln.navigation.SourcesKey
import dev.involvex.kiln.navigation.TermuxKey
import dev.involvex.kiln.navigation.TopLevelBackStack

private data class TopLevelDestination(val key: Any, val labelRes: Int, val icon: ImageVector)

@Composable
fun KilnApp(modifier: Modifier = Modifier, manageDeepLinkStore: ManageDeepLinkStore? = null) {
    val destinations = listOf(
        TopLevelDestination(AppsKey, R.string.nav_apps, Icons.Filled.Apps),
        TopLevelDestination(SourcesKey, R.string.nav_sources, Icons.Filled.CloudDownload),
        TopLevelDestination(ManageKey, R.string.nav_manage, Icons.Filled.Tune),
        TopLevelDestination(SettingsKey, R.string.nav_settings, Icons.Filled.Settings),
    )
    val topLevelBackStack = remember { TopLevelBackStack<Any>(AppsKey) }
    val scrollToTop = remember { ScrollToTopSignal() }

    if (manageDeepLinkStore != null) {
        LaunchedEffect(manageDeepLinkStore) {
            manageDeepLinkStore.pending.collect { pending ->
                if (pending && manageDeepLinkStore.consume()) {
                    topLevelBackStack.addTopLevel(ManageKey)
                }
            }
        }
    }

    CompositionLocalProvider(LocalScrollToTopSignal provides scrollToTop) {
        Scaffold(
            modifier = modifier,
            bottomBar = {
                NavigationBar {
                    destinations.forEach { destination ->
                        val selected = destination.key == topLevelBackStack.topLevelKey
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                val alreadyAtRoot = topLevelBackStack.addTopLevel(destination.key)
                                if (alreadyAtRoot) {
                                    scrollToTop.request()
                                }
                            },
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
}

private fun EntryProviderScope<Any>.kilnNavEntries(topLevelBackStack: TopLevelBackStack<Any>) {
    entry<AppsKey> {
        AppsShellRoute(
            onOpenDetail = { packageName ->
                topLevelBackStack.add(AppDetailKey(packageName))
            },
            onOpenCatalogDetail = { packageName, repoId ->
                topLevelBackStack.add(CatalogDetailKey(packageName, repoId))
            },
        )
    }
    entry<AppDetailKey> { key ->
        AppDetailRoute(
            packageName = key.packageName,
            onBack = { topLevelBackStack.removeLast() },
        )
    }
    entry<CatalogDetailKey> { key ->
        CatalogDetailRoute(
            packageName = key.packageName,
            repoId = key.repoId,
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
    entry<DiscoveryKey> {
        DiscoveryRoute(
            onBack = { topLevelBackStack.removeLast() },
            onOpenCatalogDetail = { packageName, repoId ->
                topLevelBackStack.add(CatalogDetailKey(packageName, repoId))
            },
        )
    }
    entry<ManageKey> { ManageRoute() }
    entry<SettingsKey> {
        SettingsRoute(
            onOpenPrivilegeSetup = { topLevelBackStack.add(PrivilegeSetupKey) },
            onOpenDebloat = { topLevelBackStack.add(DebloatKey) },
            onOpenTermux = { topLevelBackStack.add(TermuxKey) },
            onOpenActivityLog = { topLevelBackStack.add(ActivityLogKey) },
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
    entry<ActivityLogKey> {
        ActivityLogRoute(onBack = { topLevelBackStack.removeLast() })
    }
}

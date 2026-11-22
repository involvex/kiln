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
import dev.involvex.kiln.feature.apps.UpdatesDeepLinkStore
import dev.involvex.kiln.feature.apps.apktools.IncomingApkInstallStore
import dev.involvex.kiln.feature.debloat.DebloatRoute
import dev.involvex.kiln.feature.onboarding.AutoMatchRoute
import dev.involvex.kiln.feature.onboarding.privilege.PrivilegeSetupRoute
import dev.involvex.kiln.feature.permissions.PermissionAuditRoute
import dev.involvex.kiln.feature.permissions.background.BackgroundControlsRoute
import dev.involvex.kiln.feature.permissions.freeze.FreezeRoute
import dev.involvex.kiln.feature.settings.SettingsAboutRoute
import dev.involvex.kiln.feature.settings.SettingsAppearanceRoute
import dev.involvex.kiln.feature.settings.SettingsAutomationRoute
import dev.involvex.kiln.feature.settings.SettingsDataRoute
import dev.involvex.kiln.feature.settings.SettingsIgnoredUpdatesRoute
import dev.involvex.kiln.feature.settings.SettingsInstallationRoute
import dev.involvex.kiln.feature.settings.SettingsMaintenanceRoute
import dev.involvex.kiln.feature.settings.SettingsNetworkRoute
import dev.involvex.kiln.feature.settings.SettingsRoute
import dev.involvex.kiln.feature.settings.SettingsToolsRoute
import dev.involvex.kiln.feature.settings.SettingsUpdatesRoute
import dev.involvex.kiln.feature.settings.activity.ActivityLogRoute
import dev.involvex.kiln.feature.sources.SourcesImportRoute
import dev.involvex.kiln.feature.sources.SourcesReposRoute
import dev.involvex.kiln.feature.sources.SourcesRoute
import dev.involvex.kiln.feature.sources.SourcesStarsRoute
import dev.involvex.kiln.feature.sources.SourcesWatchlistRoute
import dev.involvex.kiln.feature.sources.discovery.CatalogDetailRoute
import dev.involvex.kiln.feature.sources.discovery.DiscoveryRoute
import dev.involvex.kiln.feature.termux.TermuxRoute
import dev.involvex.kiln.feature.widgets.ManageDeepLinkStore
import dev.involvex.kiln.feature.widgets.PrivilegeSetupDeepLinkStore
import dev.involvex.kiln.navigation.ActivityLogKey
import dev.involvex.kiln.navigation.AppDetailKey
import dev.involvex.kiln.navigation.AppsKey
import dev.involvex.kiln.navigation.AutoMatchKey
import dev.involvex.kiln.navigation.BackgroundControlsKey
import dev.involvex.kiln.navigation.CatalogDetailKey
import dev.involvex.kiln.navigation.DebloatKey
import dev.involvex.kiln.navigation.DiscoveryKey
import dev.involvex.kiln.navigation.FreezeKey
import dev.involvex.kiln.navigation.ManageKey
import dev.involvex.kiln.navigation.PermissionAuditKey
import dev.involvex.kiln.navigation.PrivilegeSetupKey
import dev.involvex.kiln.navigation.SettingsAboutKey
import dev.involvex.kiln.navigation.SettingsAppearanceKey
import dev.involvex.kiln.navigation.SettingsAutomationKey
import dev.involvex.kiln.navigation.SettingsDataKey
import dev.involvex.kiln.navigation.SettingsIgnoredUpdatesKey
import dev.involvex.kiln.navigation.SettingsInstallationKey
import dev.involvex.kiln.navigation.SettingsKey
import dev.involvex.kiln.navigation.SettingsMaintenanceKey
import dev.involvex.kiln.navigation.SettingsNetworkKey
import dev.involvex.kiln.navigation.SettingsToolsKey
import dev.involvex.kiln.navigation.SettingsUpdatesKey
import dev.involvex.kiln.navigation.SourcesImportKey
import dev.involvex.kiln.navigation.SourcesKey
import dev.involvex.kiln.navigation.SourcesReposKey
import dev.involvex.kiln.navigation.SourcesStarsKey
import dev.involvex.kiln.navigation.SourcesWatchlistKey
import dev.involvex.kiln.navigation.TermuxKey
import dev.involvex.kiln.navigation.TopLevelBackStack

private data class TopLevelDestination(val key: Any, val labelRes: Int, val icon: ImageVector)

@Composable
fun KilnApp(
    modifier: Modifier = Modifier,
    manageDeepLinkStore: ManageDeepLinkStore? = null,
    privilegeSetupDeepLinkStore: PrivilegeSetupDeepLinkStore? = null,
    updatesDeepLinkStore: UpdatesDeepLinkStore? = null,
    incomingApkInstallStore: IncomingApkInstallStore? = null,
) {
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

    if (privilegeSetupDeepLinkStore != null) {
        LaunchedEffect(privilegeSetupDeepLinkStore) {
            privilegeSetupDeepLinkStore.pending.collect { pending ->
                if (pending && privilegeSetupDeepLinkStore.consume()) {
                    topLevelBackStack.add(PrivilegeSetupKey)
                }
            }
        }
    }

    // Do not consume here — AppsViewModel applies the Updates filter and clears the flag.
    if (updatesDeepLinkStore != null) {
        LaunchedEffect(updatesDeepLinkStore) {
            updatesDeepLinkStore.pending.collect { pending ->
                if (pending) {
                    topLevelBackStack.addTopLevel(AppsKey)
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
                    kilnNavEntries(topLevelBackStack, incomingApkInstallStore)
                },
            )
        }
    }
}

@Suppress("LongMethod")
private fun EntryProviderScope<Any>.kilnNavEntries(
    topLevelBackStack: TopLevelBackStack<Any>,
    incomingApkInstallStore: IncomingApkInstallStore?,
) {
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
            onOpenRepos = { topLevelBackStack.add(SourcesReposKey) },
            onOpenWatchlist = { topLevelBackStack.add(SourcesWatchlistKey) },
            onOpenStars = { topLevelBackStack.add(SourcesStarsKey) },
            onOpenImport = { topLevelBackStack.add(SourcesImportKey) },
            onOpenAutoMatch = { topLevelBackStack.add(AutoMatchKey) },
        )
    }
    entry<SourcesReposKey> {
        SourcesReposRoute(onBack = { topLevelBackStack.removeLast() })
    }
    entry<SourcesWatchlistKey> {
        SourcesWatchlistRoute(onBack = { topLevelBackStack.removeLast() })
    }
    entry<SourcesStarsKey> {
        SourcesStarsRoute(
            onBack = { topLevelBackStack.removeLast() },
            onOpenGithubTokenSettings = {
                topLevelBackStack.addTopLevel(SettingsKey)
                topLevelBackStack.add(SettingsUpdatesKey)
            },
        )
    }
    entry<SourcesImportKey> {
        SourcesImportRoute(onBack = { topLevelBackStack.removeLast() })
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
    entry<ManageKey> {
        ManageRoute(
            onOpenPrivilegeSetup = { topLevelBackStack.add(PrivilegeSetupKey) },
            incomingApkInstallStore = incomingApkInstallStore,
        )
    }
    entry<SettingsKey> {
        SettingsRoute(
            onOpenAppearance = { topLevelBackStack.add(SettingsAppearanceKey) },
            onOpenUpdates = { topLevelBackStack.add(SettingsUpdatesKey) },
            onOpenNetwork = { topLevelBackStack.add(SettingsNetworkKey) },
            onOpenInstallation = { topLevelBackStack.add(SettingsInstallationKey) },
            onOpenAutomation = { topLevelBackStack.add(SettingsAutomationKey) },
            onOpenMaintenance = { topLevelBackStack.add(SettingsMaintenanceKey) },
            onOpenTools = { topLevelBackStack.add(SettingsToolsKey) },
            onOpenData = { topLevelBackStack.add(SettingsDataKey) },
            onOpenAbout = { topLevelBackStack.add(SettingsAboutKey) },
        )
    }
    entry<SettingsAppearanceKey> {
        SettingsAppearanceRoute(onBack = { topLevelBackStack.removeLast() })
    }
    entry<SettingsUpdatesKey> {
        SettingsUpdatesRoute(
            onBack = { topLevelBackStack.removeLast() },
            onOpenIgnoredUpdates = { topLevelBackStack.add(SettingsIgnoredUpdatesKey) },
        )
    }
    entry<SettingsIgnoredUpdatesKey> {
        SettingsIgnoredUpdatesRoute(
            onBack = { topLevelBackStack.removeLast() },
            onOpenAppDetail = { packageName -> topLevelBackStack.add(AppDetailKey(packageName)) },
        )
    }
    entry<SettingsNetworkKey> {
        SettingsNetworkRoute(onBack = { topLevelBackStack.removeLast() })
    }
    entry<SettingsInstallationKey> {
        SettingsInstallationRoute(onBack = { topLevelBackStack.removeLast() })
    }
    entry<SettingsAutomationKey> {
        SettingsAutomationRoute(onBack = { topLevelBackStack.removeLast() })
    }
    entry<SettingsMaintenanceKey> {
        SettingsMaintenanceRoute(onBack = { topLevelBackStack.removeLast() })
    }
    entry<SettingsToolsKey> {
        SettingsToolsRoute(
            onBack = { topLevelBackStack.removeLast() },
            onOpenPrivilegeSetup = { topLevelBackStack.add(PrivilegeSetupKey) },
            onOpenDebloat = { topLevelBackStack.add(DebloatKey) },
            onOpenPermissionAudit = { topLevelBackStack.add(PermissionAuditKey) },
            onOpenBackgroundControls = { topLevelBackStack.add(BackgroundControlsKey) },
            onOpenFreeze = { topLevelBackStack.add(FreezeKey) },
            onOpenTermux = { topLevelBackStack.add(TermuxKey) },
            onOpenActivityLog = { topLevelBackStack.add(ActivityLogKey) },
        )
    }
    entry<SettingsDataKey> {
        SettingsDataRoute(onBack = { topLevelBackStack.removeLast() })
    }
    entry<SettingsAboutKey> {
        SettingsAboutRoute(onBack = { topLevelBackStack.removeLast() })
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
    entry<PermissionAuditKey> {
        PermissionAuditRoute(
            onBack = { topLevelBackStack.removeLast() },
            onOpenPrivilegeSetup = { topLevelBackStack.add(PrivilegeSetupKey) },
        )
    }
    entry<BackgroundControlsKey> {
        BackgroundControlsRoute(
            onBack = { topLevelBackStack.removeLast() },
            onOpenPrivilegeSetup = { topLevelBackStack.add(PrivilegeSetupKey) },
        )
    }
    entry<FreezeKey> {
        FreezeRoute(
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

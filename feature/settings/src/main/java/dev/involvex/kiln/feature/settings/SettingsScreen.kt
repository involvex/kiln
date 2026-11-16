package dev.involvex.kiln.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.involvex.kiln.core.designsystem.component.KilnScaffold
import dev.involvex.kiln.core.designsystem.kilnHiltViewModel
import dev.involvex.kiln.core.designsystem.scroll.ScrollToTopEffect

@Composable
fun SettingsRoute(
    onOpenAppearance: () -> Unit,
    onOpenUpdates: () -> Unit,
    onOpenNetwork: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenInstallation: () -> Unit,
    onOpenAutomation: () -> Unit,
    onOpenMaintenance: () -> Unit,
    onOpenTools: () -> Unit,
    onOpenData: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = kilnHiltViewModel<SettingsViewModel>()
    val title = stringResource(R.string.feature_settings_title)
    val description = stringResource(R.string.feature_settings_description)
    LaunchedEffect(title, description) {
        viewModel.onScreenShown(title, description)
    }
    SettingsHubScreen(
        destinations = settingsHubDestinations(
            onOpenAppearance = onOpenAppearance,
            onOpenUpdates = onOpenUpdates,
            onOpenNetwork = onOpenNetwork,
            onOpenPrivacy = onOpenPrivacy,
            onOpenInstallation = onOpenInstallation,
            onOpenAutomation = onOpenAutomation,
            onOpenMaintenance = onOpenMaintenance,
            onOpenTools = onOpenTools,
            onOpenData = onOpenData,
            onOpenAbout = onOpenAbout,
        ),
        modifier = modifier,
    )
}

data class SettingsHubDestination(val titleRes: Int, val subtitleRes: Int, val onClick: () -> Unit)

fun settingsHubDestinations(
    onOpenAppearance: () -> Unit,
    onOpenUpdates: () -> Unit,
    onOpenNetwork: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenInstallation: () -> Unit,
    onOpenAutomation: () -> Unit,
    onOpenMaintenance: () -> Unit,
    onOpenTools: () -> Unit,
    onOpenData: () -> Unit,
    onOpenAbout: () -> Unit,
): List<SettingsHubDestination> = listOf(
    SettingsHubDestination(R.string.settings_hub_appearance, R.string.settings_hub_appearance_sub, onOpenAppearance),
    SettingsHubDestination(R.string.settings_hub_updates, R.string.settings_hub_updates_sub, onOpenUpdates),
    SettingsHubDestination(R.string.settings_hub_network, R.string.settings_hub_network_sub, onOpenNetwork),
    SettingsHubDestination(R.string.settings_hub_privacy, R.string.settings_hub_privacy_sub, onOpenPrivacy),
    SettingsHubDestination(R.string.settings_hub_installation, R.string.settings_hub_installation_sub, onOpenInstallation),
    SettingsHubDestination(R.string.settings_hub_automation, R.string.settings_hub_automation_sub, onOpenAutomation),
    SettingsHubDestination(R.string.settings_hub_maintenance, R.string.settings_hub_maintenance_sub, onOpenMaintenance),
    SettingsHubDestination(R.string.settings_hub_tools, R.string.settings_hub_tools_sub, onOpenTools),
    SettingsHubDestination(R.string.settings_hub_data, R.string.settings_hub_data_sub, onOpenData),
    SettingsHubDestination(R.string.settings_hub_about, R.string.settings_hub_about_sub, onOpenAbout),
)

@Composable
fun SettingsHubScreen(destinations: List<SettingsHubDestination>, modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()
    ScrollToTopEffect(scrollState)
    KilnScaffold(
        title = stringResource(R.string.feature_settings_title),
        modifier = modifier,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.feature_settings_description),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            destinations.forEachIndexed { index, entry ->
                if (index > 0) HorizontalDivider()
                SettingsHubRow(
                    title = stringResource(entry.titleRes),
                    subtitle = stringResource(entry.subtitleRes),
                    onClick = entry.onClick,
                )
            }
        }
    }
}

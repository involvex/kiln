package dev.involvex.kiln.feature.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SettingsAutomationRoute(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsCategoryScaffold(
        title = stringResource(R.string.settings_hub_automation),
        onBack = onBack,
        modifier = modifier,
    ) { padding ->
        SettingsScrollColumn(padding = padding) {
            Text(
                text = stringResource(R.string.settings_automation_explain),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SettingsSwitchRow(
                title = stringResource(R.string.settings_automation_enabled),
                checked = state.automation.enabled,
                onCheckedChange = viewModel::setAutomationEnabled,
            )
        }
    }
}

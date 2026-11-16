package dev.involvex.kiln.feature.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.involvex.kiln.core.designsystem.kilnHiltViewModel
import kotlinx.coroutines.delay

@Composable
fun SettingsPrivacyRoute(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel = kilnHiltViewModel<SettingsViewModel>()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsCategoryScaffold(
        title = stringResource(R.string.settings_hub_privacy),
        onBack = onBack,
        modifier = modifier,
    ) { padding ->
        SettingsScrollColumn(padding = padding) {
            ExodusPrivacySection(
                prefs = state.exodusPrivacy,
                baseUrlInput = state.exodusBaseUrlInput,
                tokenInput = state.exodusTokenInput,
                onBaseUrlChange = viewModel::onExodusBaseUrlInputChange,
                onTokenChange = viewModel::onExodusTokenInputChange,
                onSave = viewModel::saveExodusApi,
                onClearToken = viewModel::clearExodusToken,
            )
            state.configMessage?.let { key ->
                val text = when (key) {
                    "exodus_saved" -> stringResource(R.string.settings_exodus_saved)
                    "exodus_token_cleared" -> stringResource(R.string.settings_exodus_token_cleared)
                    "exodus_https_required" -> stringResource(R.string.settings_exodus_https_required)
                    else -> null
                }
                if (text != null) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    LaunchedEffect(key) {
                        delay(4_000)
                        viewModel.consumeConfigMessage()
                    }
                }
            }
        }
    }
}

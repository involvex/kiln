package dev.involvex.kiln.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.involvex.kiln.core.model.PreferredInstaller

@Composable
fun SettingsInstallationRoute(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsCategoryScaffold(
        title = stringResource(R.string.settings_hub_installation),
        onBack = onBack,
        modifier = modifier,
    ) { padding ->
        SettingsScrollColumn(padding = padding) {
            Text(
                text = stringResource(R.string.settings_installer_explain),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = state.preferredInstaller == PreferredInstaller.SYSTEM,
                    onClick = { viewModel.setPreferredInstaller(PreferredInstaller.SYSTEM) },
                    label = { Text(stringResource(R.string.settings_installer_system)) },
                )
                FilterChip(
                    selected = state.preferredInstaller == PreferredInstaller.SHIZUKU,
                    onClick = { viewModel.setPreferredInstaller(PreferredInstaller.SHIZUKU) },
                    label = { Text(stringResource(R.string.settings_installer_shizuku)) },
                )
            }
            if (state.preferredInstaller == PreferredInstaller.SHIZUKU && !state.shizukuSilentAvailable) {
                Text(
                    text = stringResource(R.string.settings_installer_shizuku_unavailable),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

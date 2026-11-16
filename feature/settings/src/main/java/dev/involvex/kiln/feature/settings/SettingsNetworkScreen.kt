package dev.involvex.kiln.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.involvex.kiln.core.designsystem.kilnHiltViewModel
import dev.involvex.kiln.core.model.NetworkProxyPrefs
import dev.involvex.kiln.core.model.NetworkProxyType

@Composable
fun SettingsNetworkRoute(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel = kilnHiltViewModel<SettingsViewModel>()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsCategoryScaffold(
        title = stringResource(R.string.settings_hub_network),
        onBack = onBack,
        modifier = modifier,
    ) { padding ->
        SettingsScrollColumn(padding = padding) {
            NetworkProxySection(
                prefs = state.networkProxy,
                onTypeChange = viewModel::setProxyType,
                onHostChange = viewModel::setProxyHost,
                onPortChange = viewModel::setProxyPort,
                onUsernameChange = viewModel::setProxyUsername,
                onPasswordChange = viewModel::setProxyPassword,
                onOrbotChange = viewModel::setOrbotPreset,
            )
        }
    }
}

@Composable
internal fun NetworkProxySection(
    prefs: NetworkProxyPrefs,
    onTypeChange: (NetworkProxyType) -> Unit,
    onHostChange: (String) -> Unit,
    onPortChange: (Int) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onOrbotChange: (Boolean) -> Unit,
) {
    Text(
        text = stringResource(R.string.settings_network_explain),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    SettingsSwitchRow(
        title = stringResource(R.string.settings_network_orbot),
        checked = prefs.orbotPreset,
        onCheckedChange = onOrbotChange,
    )
    if (!prefs.orbotPreset) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = prefs.type == NetworkProxyType.NONE,
                onClick = { onTypeChange(NetworkProxyType.NONE) },
                label = { Text(stringResource(R.string.settings_network_type_none)) },
            )
            FilterChip(
                selected = prefs.type == NetworkProxyType.HTTP,
                onClick = { onTypeChange(NetworkProxyType.HTTP) },
                label = { Text(stringResource(R.string.settings_network_type_http)) },
            )
            FilterChip(
                selected = prefs.type == NetworkProxyType.SOCKS,
                onClick = { onTypeChange(NetworkProxyType.SOCKS) },
                label = { Text(stringResource(R.string.settings_network_type_socks)) },
            )
        }
        if (prefs.type != NetworkProxyType.NONE) {
            OutlinedTextField(
                value = prefs.host,
                onValueChange = onHostChange,
                label = { Text(stringResource(R.string.settings_network_host)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = if (prefs.port == 0) "" else prefs.port.toString(),
                onValueChange = { raw -> onPortChange(raw.filter { it.isDigit() }.toIntOrNull() ?: 0) },
                label = { Text(stringResource(R.string.settings_network_port)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = prefs.username,
                onValueChange = onUsernameChange,
                label = { Text(stringResource(R.string.settings_network_username)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = prefs.password,
                onValueChange = onPasswordChange,
                label = { Text(stringResource(R.string.settings_network_password)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
            )
        }
    } else {
        Text(
            text = stringResource(R.string.settings_network_orbot_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

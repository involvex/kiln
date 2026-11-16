package dev.involvex.kiln.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SettingsIgnoredUpdatesRoute(
    onBack: () -> Unit,
    onOpenAppDetail: (packageName: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsIgnoredUpdatesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsCategoryScaffold(
        title = stringResource(R.string.settings_ignored_updates),
        onBack = onBack,
        modifier = modifier,
    ) { padding ->
        SettingsScrollColumn(padding = padding) {
            Text(
                text = stringResource(R.string.settings_ignored_updates_explain),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (state.apps.isEmpty()) {
                Text(
                    text = stringResource(R.string.settings_ignored_updates_empty),
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                state.apps.forEach { app ->
                    IgnoredAppRow(
                        label = app.label,
                        packageName = app.packageName,
                        onOpen = { onOpenAppDetail(app.packageName) },
                        onReenable = { viewModel.reenable(app.packageName) },
                    )
                }
            }
        }
    }
}

@Composable
private fun IgnoredAppRow(label: String, packageName: String, onOpen: () -> Unit, onReenable: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = packageName,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(onClick = onReenable) {
                Text(stringResource(R.string.settings_ignored_updates_reenable))
            }
        }
    }
}

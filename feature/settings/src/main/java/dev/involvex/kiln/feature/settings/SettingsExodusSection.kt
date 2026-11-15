package dev.involvex.kiln.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.involvex.kiln.core.model.ExodusPrivacyPrefs

@Composable
internal fun ExodusPrivacySection(
    prefs: ExodusPrivacyPrefs,
    baseUrlInput: String,
    tokenInput: String,
    onBaseUrlChange: (String) -> Unit,
    onTokenChange: (String) -> Unit,
    onSave: () -> Unit,
    onClearToken: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.settings_exodus_section), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.settings_exodus_explain),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = baseUrlInput,
            onValueChange = onBaseUrlChange,
            label = { Text(stringResource(R.string.settings_exodus_base_url)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            value = tokenInput,
            onValueChange = onTokenChange,
            label = { Text(stringResource(R.string.settings_exodus_token)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Text(
            text = if (prefs.tokenSet) {
                stringResource(R.string.settings_exodus_token_set)
            } else {
                stringResource(R.string.settings_exodus_token_unset)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.settings_exodus_save))
        }
        TextButton(onClick = onClearToken) {
            Text(stringResource(R.string.settings_exodus_clear_token))
        }
    }
}

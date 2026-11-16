package dev.involvex.kiln.feature.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SettingsAboutRoute(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    SettingsCategoryScaffold(
        title = stringResource(R.string.settings_hub_about),
        onBack = onBack,
        modifier = modifier,
    ) { padding ->
        SettingsScrollColumn(padding = padding) {
            Text(
                text = stringResource(
                    R.string.settings_about_version,
                    state.appVersionName.ifBlank { stringResource(R.string.settings_about_version_unknown) },
                    state.appVersionCode,
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(R.string.settings_about_license),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SettingsSwitchRow(
                title = stringResource(R.string.settings_dev_debug_log),
                checked = state.devPrefs.debugLogEnabled,
                onCheckedChange = viewModel::setDebugLogEnabled,
            )
            Text(
                text = stringResource(R.string.settings_dev_debug_log_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val sourceLabel = stringResource(R.string.settings_about_source)
            OutlinedButton(
                onClick = { openHttps(context, URL_SOURCE) },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = sourceLabel },
            ) {
                Text(sourceLabel)
            }
            val privacyLabel = stringResource(R.string.settings_about_privacy)
            OutlinedButton(
                onClick = { openHttps(context, URL_PRIVACY) },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = privacyLabel },
            ) {
                Text(privacyLabel)
            }
            val supportLabel = stringResource(R.string.settings_about_support)
            OutlinedButton(
                onClick = { openHttps(context, URL_SUPPORTING) },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = supportLabel },
            ) {
                Text(supportLabel)
            }
            val licenseLabel = stringResource(R.string.settings_about_license_link)
            OutlinedButton(
                onClick = { openHttps(context, URL_LICENSE) },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = licenseLabel },
            ) {
                Text(licenseLabel)
            }
        }
    }
}

private fun openHttps(context: android.content.Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    runCatching { context.startActivity(intent) }
}

private const val URL_SOURCE: String = "https://github.com/involvex/kiln"
private const val URL_PRIVACY: String =
    "https://github.com/involvex/kiln/blob/main/docs/PRIVACY.md"
private const val URL_SUPPORTING: String =
    "https://github.com/involvex/kiln/blob/main/docs/SUPPORTING.md"
private const val URL_LICENSE: String =
    "https://github.com/involvex/kiln/blob/main/LICENSE"

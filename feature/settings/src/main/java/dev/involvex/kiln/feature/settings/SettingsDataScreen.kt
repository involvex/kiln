package dev.involvex.kiln.feature.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.involvex.kiln.core.model.KilnConfigFilenames
import dev.involvex.kiln.core.model.KilnConfigImportPreview
import kotlinx.coroutines.delay

@Composable
fun SettingsDataRoute(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val exportConfig = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(KilnConfigFilenames.MIME_JSON),
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.exportConfigToUri(uri, context.contentResolver)
        }
    }
    val importConfig = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.previewImportFromUri(uri, context.contentResolver)
        }
    }
    SettingsCategoryScaffold(
        title = stringResource(R.string.settings_hub_data),
        onBack = onBack,
        modifier = modifier,
    ) { padding ->
        SettingsScrollColumn(padding = padding) {
            Text(
                text = stringResource(R.string.settings_config_explain),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = { exportConfig.launch(viewModel.suggestedConfigFileName()) },
                enabled = !state.configBusy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.settings_config_export))
            }
            OutlinedButton(
                onClick = {
                    importConfig.launch(
                        arrayOf(KilnConfigFilenames.MIME_JSON, "application/octet-stream", "text/*", "*/*"),
                    )
                },
                enabled = !state.configBusy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.settings_config_import))
            }
            if (state.configBusy) {
                Text(
                    text = stringResource(R.string.settings_config_busy),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            state.configMessage?.let { key ->
                Text(
                    text = configMessageText(key),
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
    state.pendingImportPreview?.let { preview ->
        ImportConfirmDialog(
            preview = preview,
            onConfirm = viewModel::confirmPendingImport,
            onDismiss = viewModel::dismissImportPreview,
        )
    }
}

@Composable
private fun configMessageText(key: String): String = when {
    key == "config_export_ok" -> stringResource(R.string.settings_config_export_ok)

    key == "config_export_write_failed" -> stringResource(R.string.settings_config_export_failed)

    key == "config_import_read_failed" -> stringResource(R.string.settings_config_import_read_failed)

    key.startsWith("config_export_failed:") -> stringResource(R.string.settings_config_export_failed)

    key.startsWith("config_import_failed:") -> stringResource(R.string.settings_config_import_failed)

    key.startsWith("config_import_ok:") -> {
        val parts = key.removePrefix("config_import_ok:").split(":")
        stringResource(
            R.string.settings_config_import_ok,
            parts.getOrNull(0) ?: "0",
            parts.getOrNull(1) ?: "0",
            parts.getOrNull(2) ?: "0",
        )
    }

    else -> key
}

@Composable
private fun ImportConfirmDialog(preview: KilnConfigImportPreview, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val prefsLine = if (preview.prefsSections.isEmpty()) {
        stringResource(R.string.settings_config_import_prefs_none)
    } else {
        stringResource(
            R.string.settings_config_import_prefs,
            preview.prefsSections.joinToString(", "),
        )
    }
    val patHint = if (preview.githubPatWasConfigured) {
        stringResource(R.string.settings_config_import_pat_hint)
    } else {
        ""
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_config_import_confirm_title)) },
        text = {
            Text(
                stringResource(
                    R.string.settings_config_import_confirm_body,
                    preview.reposNew,
                    preview.reposUpdated,
                    preview.managedAppsUpserted,
                    prefsLine,
                    patHint,
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.settings_config_import_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_config_import_cancel))
            }
        },
    )
}

package dev.involvex.kiln.feature.apps.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.involvex.kiln.core.model.ApkExportFormat
import dev.involvex.kiln.feature.apps.ExportDialogState
import dev.involvex.kiln.feature.apps.R

@Composable
fun ExportFormatDialog(
    state: ExportDialogState,
    inProgress: Boolean,
    onFormatChange: (ApkExportFormat) -> Unit,
    onDismiss: () -> Unit,
    onConfirmSave: () -> Unit,
    onConfirmShare: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!inProgress) onDismiss() },
        title = { Text(stringResource(R.string.feature_apps_export_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.warnBaseOnly) {
                    Text(stringResource(R.string.feature_apps_export_split_warning))
                }
                Text(stringResource(R.string.feature_apps_export_format))
                FormatRadio(
                    ApkExportFormat.APK,
                    state.format,
                    onFormatChange,
                    stringResource(R.string.feature_apps_export_apk),
                )
                FormatRadio(
                    ApkExportFormat.APKS,
                    state.format,
                    onFormatChange,
                    stringResource(R.string.feature_apps_export_apks),
                )
                FormatRadio(
                    ApkExportFormat.XAPK,
                    state.format,
                    onFormatChange,
                    stringResource(R.string.feature_apps_export_xapk),
                )
                if (inProgress) {
                    CircularProgressIndicator(modifier = Modifier.padding(top = 8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirmSave, enabled = !inProgress) {
                Text(stringResource(R.string.feature_apps_export_confirm))
            }
        },
        dismissButton = {
            Row {
                if (!state.isBatch) {
                    TextButton(onClick = onConfirmShare, enabled = !inProgress) {
                        Text(stringResource(R.string.feature_apps_export_share))
                    }
                }
                TextButton(onClick = onDismiss, enabled = !inProgress) {
                    Text(stringResource(R.string.feature_apps_export_cancel))
                }
            }
        },
    )
}

@Composable
fun NotificationRationaleDialog(onContinue: () -> Unit, onSkip: () -> Unit) {
    AlertDialog(
        onDismissRequest = onSkip,
        title = { Text(stringResource(R.string.feature_apps_notification_rationale_title)) },
        text = { Text(stringResource(R.string.feature_apps_notification_rationale_body)) },
        confirmButton = {
            TextButton(onClick = onContinue) {
                Text(stringResource(R.string.feature_apps_notification_rationale_continue))
            }
        },
        dismissButton = {
            TextButton(onClick = onSkip) {
                Text(stringResource(R.string.feature_apps_export_cancel))
            }
        },
    )
}

@Composable
private fun FormatRadio(format: ApkExportFormat, selected: ApkExportFormat, onSelect: (ApkExportFormat) -> Unit, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(format) },
    ) {
        RadioButton(selected = selected == format, onClick = { onSelect(format) })
        Text(label)
    }
}

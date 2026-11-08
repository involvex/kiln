package dev.involvex.kiln.feature.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.involvex.kiln.core.common.ByteSizeFormatter
import dev.involvex.kiln.core.designsystem.component.KilnScaffold
import dev.involvex.kiln.core.model.BackupAppCandidate
import dev.involvex.kiln.core.model.BackupContentTier

@Composable
fun BackupRoute(
    modifier: Modifier = Modifier,
    showTopBar: Boolean = true,
    viewModel: BackupViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val createDoc = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.performBackup(uri, context.contentResolver)
        } else {
            viewModel.dismissBackup()
        }
    }
    val openDoc = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.performRestore(uri, context.contentResolver)
        } else {
            viewModel.dismissRestore()
        }
    }

    BackupScreen(
        state = state,
        onRefresh = viewModel::refresh,
        onQueryChange = viewModel::setQuery,
        onSelect = viewModel::select,
        onRequestBackup = {
            val pkg = state.selectedPackage ?: return@BackupScreen
            viewModel.requestBackup()
            createDoc.launch(viewModel.suggestedFileName(pkg))
        },
        onRequestRestore = viewModel::requestRestore,
        onConfirmRestore = {
            viewModel.dismissRestore()
            openDoc.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
        },
        onDismissRestore = viewModel::dismissRestore,
        showTopBar = showTopBar,
        modifier = modifier,
    )
}

@Composable
fun BackupScreen(
    state: BackupUiState,
    onRefresh: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSelect: (String) -> Unit,
    onRequestBackup: () -> Unit,
    onRequestRestore: () -> Unit,
    onConfirmRestore: () -> Unit,
    onDismissRestore: () -> Unit,
    modifier: Modifier = Modifier,
    showTopBar: Boolean = true,
) {
    KilnScaffold(
        title = stringResource(R.string.feature_backup_title),
        modifier = modifier,
        showTopBar = showTopBar,
    ) { padding ->
        if (state.isLoading && state.apps.isEmpty()) {
            BackupLoading(Modifier.padding(padding))
            return@KilnScaffold
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            backupListItems(
                state = state,
                onRefresh = onRefresh,
                onQueryChange = onQueryChange,
                onSelect = onSelect,
                onRequestBackup = onRequestBackup,
                onRequestRestore = onRequestRestore,
            )
        }
    }

    if (state.pendingRestore) {
        RestoreConfirmDialog(
            onConfirm = onConfirmRestore,
            onDismiss = onDismissRestore,
        )
    }
}

@Composable
private fun BackupLoading(modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun RestoreConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.backup_restore_confirm_title)) },
        text = { Text(stringResource(R.string.backup_restore_confirm_body)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.backup_restore_confirm_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.backup_cancel))
            }
        },
    )
}

private fun LazyListScope.backupListItems(
    state: BackupUiState,
    onRefresh: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSelect: (String) -> Unit,
    onRequestBackup: () -> Unit,
    onRequestRestore: () -> Unit,
) {
    item { BackupHonestyBanner(tier = state.tier) }
    item {
        BackupActions(
            canBackup = state.selectedPackage != null && !state.busy,
            busy = state.busy,
            onBackup = onRequestBackup,
            onRestore = onRequestRestore,
            onRefresh = onRefresh,
        )
    }
    item {
        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(stringResource(R.string.backup_search)) },
        )
    }
    state.message?.let { msg ->
        item {
            Text(text = userMessage(msg), style = MaterialTheme.typography.bodySmall)
        }
    }
    if (state.busy) {
        item { CircularProgressIndicator() }
    }
    items(state.filteredApps, key = { it.packageName }) { app ->
        BackupAppRow(
            app = app,
            selected = app.packageName == state.selectedPackage,
            onSelect = { onSelect(app.packageName) },
        )
    }
}

@Composable
private fun BackupActions(canBackup: Boolean, busy: Boolean, onBackup: () -> Unit, onRestore: () -> Unit, onRefresh: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onBackup, enabled = canBackup) {
            Text(stringResource(R.string.backup_action_backup))
        }
        OutlinedButton(onClick = onRestore, enabled = !busy) {
            Text(stringResource(R.string.backup_action_restore))
        }
        OutlinedButton(onClick = onRefresh) {
            Text(stringResource(R.string.backup_refresh))
        }
    }
}

@Composable
private fun BackupHonestyBanner(tier: BackupContentTier) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.backup_tier_line, tierLabel(tier)),
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = stringResource(R.string.backup_honesty_apk_only),
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = when (tier) {
                BackupContentTier.NONE -> stringResource(R.string.backup_honesty_none)
                BackupContentTier.SHIZUKU -> stringResource(R.string.backup_honesty_shizuku)
                BackupContentTier.ROOT -> stringResource(R.string.backup_honesty_root)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BackupAppRow(app: BackupAppCandidate, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Column(modifier = Modifier.weight(1f)) {
            Text(app.label, style = MaterialTheme.typography.titleSmall)
            Text(app.packageName, style = MaterialTheme.typography.bodySmall)
            Text(
                text = stringResource(
                    R.string.backup_app_meta,
                    app.versionName ?: app.versionCode.toString(),
                    ByteSizeFormatter.format(app.apkSizeBytes),
                    if (app.hasSplits) stringResource(R.string.backup_has_splits) else "",
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun tierLabel(tier: BackupContentTier): String = when (tier) {
    BackupContentTier.NONE -> stringResource(R.string.backup_tier_none)
    BackupContentTier.SHIZUKU -> stringResource(R.string.backup_tier_shizuku)
    BackupContentTier.ROOT -> stringResource(R.string.backup_tier_root)
}

@Composable
private fun userMessage(raw: String): String = when {
    raw.startsWith("backup_ok:") ->
        stringResource(R.string.backup_msg_ok, raw.removePrefix("backup_ok:"))

    raw.startsWith("restore_queued:") -> stringResource(R.string.backup_msg_restore_queued)

    raw == "package_not_found" -> stringResource(R.string.backup_err_package_not_found)

    raw == "apk_unreadable" || raw == "apk_missing" -> stringResource(R.string.backup_err_apk)

    raw == "metadata_missing" -> stringResource(R.string.backup_err_metadata)

    raw == "could_not_open_output" || raw == "could_not_open_input" ->
        stringResource(R.string.backup_err_saf)

    else -> raw
}

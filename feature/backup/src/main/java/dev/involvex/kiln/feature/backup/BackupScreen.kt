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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.involvex.kiln.core.common.ByteSizeFormatter
import dev.involvex.kiln.core.designsystem.component.KilnScaffold
import dev.involvex.kiln.core.designsystem.kilnHiltViewModel
import dev.involvex.kiln.core.designsystem.scroll.ScrollToTopEffect
import dev.involvex.kiln.core.model.AndroidUserInfo
import dev.involvex.kiln.core.model.BackupAppCandidate
import dev.involvex.kiln.core.model.BackupContentTier
import dev.involvex.kiln.core.model.BackupPrefs
import kotlin.math.roundToInt

@Composable
fun BackupRoute(modifier: Modifier = Modifier, showTopBar: Boolean = true) {
    val viewModel = kilnHiltViewModel<BackupViewModel>()
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
            viewModel.onRestoreFilePicked(uri, context.contentResolver)
        } else {
            viewModel.dismissRestore()
        }
    }
    val openTree = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onTreePicked(uri, context.contentResolver)
        }
    }

    LaunchedEffect(state.pendingCreateDocumentName) {
        val name = state.pendingCreateDocumentName ?: return@LaunchedEffect
        viewModel.consumeCreateDocument()
        createDoc.launch(name)
    }

    BackupScreen(
        state = state,
        onRefresh = viewModel::refresh,
        onQueryChange = viewModel::setQuery,
        onSelect = viewModel::select,
        onRequestBackup = viewModel::requestBackup,
        onRequestRestore = viewModel::requestRestore,
        onConfirmRestore = {
            viewModel.confirmRestore()
            openDoc.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
        },
        onDismissRestore = viewModel::dismissRestore,
        onSelectConfirmUser = viewModel::selectConfirmUser,
        onConfirmBackupUser = viewModel::confirmBackupUser,
        onDismissBackupUser = viewModel::dismissBackupUserConfirm,
        onPickTree = { openTree.launch(null) },
        onClearTree = { viewModel.clearTree(context.contentResolver) },
        onKeepNChange = viewModel::setKeepN,
        onEncryptChange = viewModel::setEncryptEnabled,
        onPasswordDraftChange = viewModel::setPasswordDraft,
        onConfirmPassword = { viewModel.confirmPasswordPrompt(context.contentResolver) },
        onDismissPassword = viewModel::dismissPasswordPrompt,
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
    onSelectConfirmUser: (Int) -> Unit,
    onConfirmBackupUser: () -> Unit,
    onDismissBackupUser: () -> Unit,
    onPickTree: () -> Unit,
    onClearTree: () -> Unit,
    onKeepNChange: (Int) -> Unit,
    onEncryptChange: (Boolean) -> Unit,
    onPasswordDraftChange: (String) -> Unit,
    onConfirmPassword: () -> Unit,
    onDismissPassword: () -> Unit,
    modifier: Modifier = Modifier,
    showTopBar: Boolean = true,
) {
    val listState = rememberLazyListState()
    ScrollToTopEffect(listState)
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
            state = listState,
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
                onPickTree = onPickTree,
                onClearTree = onClearTree,
                onKeepNChange = onKeepNChange,
                onEncryptChange = onEncryptChange,
            )
        }
    }

    if (state.pendingRestore) {
        RestoreConfirmDialog(
            users = state.confirmUsers,
            selectedUserId = state.selectedUserId,
            onSelectUser = onSelectConfirmUser,
            onConfirm = onConfirmRestore,
            onDismiss = onDismissRestore,
        )
    }
    if (state.pendingBackupUserConfirm) {
        BackupUserConfirmDialog(
            users = state.confirmUsers,
            selectedUserId = state.selectedUserId,
            onSelectUser = onSelectConfirmUser,
            onConfirm = onConfirmBackupUser,
            onDismiss = onDismissBackupUser,
        )
    }
    if (state.passwordPrompt != null) {
        PasswordDialog(
            prompt = state.passwordPrompt,
            value = state.passwordDraft,
            onValueChange = onPasswordDraftChange,
            onConfirm = onConfirmPassword,
            onDismiss = onDismissPassword,
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
private fun RestoreConfirmDialog(
    users: List<AndroidUserInfo>,
    selectedUserId: Int,
    onSelectUser: (Int) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.backup_restore_confirm_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.backup_restore_confirm_body))
                DualUserPicker(
                    users = users,
                    selectedUserId = selectedUserId,
                    bodyRes = R.string.backup_dual_user_restore_body,
                    onSelectUser = onSelectUser,
                )
            }
        },
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

@Composable
private fun BackupUserConfirmDialog(
    users: List<AndroidUserInfo>,
    selectedUserId: Int,
    onSelectUser: (Int) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.backup_dual_user_backup_title)) },
        text = {
            DualUserPicker(
                users = users,
                selectedUserId = selectedUserId,
                bodyRes = R.string.backup_dual_user_backup_body,
                onSelectUser = onSelectUser,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.backup_dual_user_backup_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.backup_cancel))
            }
        },
    )
}

@Composable
private fun DualUserPicker(users: List<AndroidUserInfo>, selectedUserId: Int, bodyRes: Int, onSelectUser: (Int) -> Unit) {
    if (users.size < 2) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(bodyRes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        users.forEach { user ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                RadioButton(
                    selected = selectedUserId == user.id,
                    onClick = { onSelectUser(user.id) },
                )
                Text(
                    text = stringResource(
                        R.string.backup_dual_user_option,
                        user.name,
                        user.id,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun PasswordDialog(
    prompt: BackupPasswordPrompt,
    value: String,
    onValueChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val title = when (prompt) {
        BackupPasswordPrompt.BACKUP -> stringResource(R.string.backup_password_backup_title)
        BackupPasswordPrompt.RESTORE -> stringResource(R.string.backup_password_restore_title)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                label = { Text(stringResource(R.string.backup_password_label)) },
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = value.isNotEmpty()) {
                Text(stringResource(R.string.backup_password_confirm))
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
    onPickTree: () -> Unit,
    onClearTree: () -> Unit,
    onKeepNChange: (Int) -> Unit,
    onEncryptChange: (Boolean) -> Unit,
) {
    item { BackupHonestyBanner(tier = state.tier) }
    item {
        BackupTreeSection(
            state = state,
            onPickTree = onPickTree,
            onClearTree = onClearTree,
            onKeepNChange = onKeepNChange,
        )
    }
    item {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = state.encryptEnabled, onCheckedChange = onEncryptChange)
            Text(stringResource(R.string.backup_encrypt_toggle))
        }
    }
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
private fun BackupTreeSection(state: BackupUiState, onPickTree: () -> Unit, onClearTree: () -> Unit, onKeepNChange: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.backup_tree_title), style = MaterialTheme.typography.titleSmall)
        Text(
            text = state.treeLabel?.let { stringResource(R.string.backup_tree_current, it) }
                ?: stringResource(R.string.backup_tree_none),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onPickTree) {
                Text(stringResource(R.string.backup_tree_pick))
            }
            if (state.hasTree) {
                OutlinedButton(onClick = onClearTree) {
                    Text(stringResource(R.string.backup_tree_clear))
                }
            }
        }
        Text(stringResource(R.string.backup_keep_n_label, state.prefs.keepN))
        Slider(
            value = state.prefs.keepN.toFloat(),
            onValueChange = { onKeepNChange(it.roundToInt()) },
            valueRange = BackupPrefs.MIN_KEEP_N.toFloat()..BackupPrefs.MAX_KEEP_N.toFloat(),
            steps = BackupPrefs.MAX_KEEP_N - BackupPrefs.MIN_KEEP_N - 1,
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
private fun userMessage(raw: String): String {
    if (raw.startsWith("backup_ok:")) {
        return stringResource(R.string.backup_msg_ok, raw.removePrefix("backup_ok:"))
    }
    if (raw.startsWith("restore_queued:")) {
        return stringResource(R.string.backup_msg_restore_queued)
    }
    return errorMessage(raw) ?: raw
}

@Composable
private fun errorMessage(raw: String): String? = when (raw) {
    "package_not_found" -> stringResource(R.string.backup_err_package_not_found)
    "apk_unreadable", "apk_missing" -> stringResource(R.string.backup_err_apk)
    "splits_incomplete" -> stringResource(R.string.backup_err_splits_incomplete)
    "metadata_missing" -> stringResource(R.string.backup_err_metadata)
    "could_not_open_output", "could_not_open_input" -> stringResource(R.string.backup_err_saf)
    "tree_not_set" -> stringResource(R.string.backup_err_tree)
    "tree_create_failed" -> stringResource(R.string.backup_err_tree_create)
    "password_required" -> stringResource(R.string.backup_err_password)
    "decrypt_failed" -> stringResource(R.string.backup_err_decrypt)
    else -> null
}

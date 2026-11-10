package dev.involvex.kiln.feature.sources

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.involvex.kiln.core.designsystem.component.KilnScaffold
import dev.involvex.kiln.core.designsystem.scroll.ScrollToTopEffect
import dev.involvex.kiln.core.model.WatchlistEntry

@Composable
fun SourcesRoute(onOpenAutoMatch: () -> Unit, modifier: Modifier = Modifier, viewModel: SourcesViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SourcesScreen(
        state = state,
        onSyncAll = viewModel::syncAll,
        onAddFieldsChange = viewModel::onAddFieldsChange,
        onAddRepo = viewModel::addRepo,
        onSaveRepoMirrors = viewModel::saveRepoMirrors,
        onObtainiumTextChange = viewModel::onObtainiumTextChange,
        onImportObtainium = viewModel::importObtainium,
        onImportObtainiumUri = viewModel::importObtainiumFromUri,
        onGithubTokenInputChange = viewModel::onGithubTokenInputChange,
        onSaveGithubToken = viewModel::saveGithubToken,
        onClearGithubToken = viewModel::clearGithubToken,
        onOpenAutoMatch = onOpenAutoMatch,
        onUnwatch = viewModel::unwatch,
        onClearMessage = viewModel::clearMessage,
        modifier = modifier,
    )
}

@Composable
fun SourcesScreen(
    state: SourcesUiState,
    onSyncAll: () -> Unit,
    onAddFieldsChange: (name: String?, address: String?, fingerprint: String?) -> Unit,
    onAddRepo: () -> Unit,
    onSaveRepoMirrors: (repoId: String, mirrorsRaw: String) -> Unit,
    onObtainiumTextChange: (String) -> Unit,
    onImportObtainium: () -> Unit,
    onImportObtainiumUri: (Uri) -> Unit,
    onGithubTokenInputChange: (String) -> Unit,
    onSaveGithubToken: () -> Unit,
    onClearGithubToken: () -> Unit,
    onOpenAutoMatch: () -> Unit,
    onUnwatch: (String) -> Unit,
    onClearMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    ScrollToTopEffect(listState)
    KilnScaffold(title = stringResource(R.string.feature_sources_title), modifier = modifier) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            sourcesHeader(state.isSyncing, onSyncAll, onOpenAutoMatch)
            sourcesWatchlist(state.watchlist, onUnwatch)
            sourcesRepoList(state.repos, onSaveRepoMirrors)
            item { AddRepoSection(state, onAddFieldsChange, onAddRepo) }
            item { GithubPatSection(state, onGithubTokenInputChange, onSaveGithubToken, onClearGithubToken) }
            item {
                ObtainiumSection(
                    obtainiumText = state.obtainiumText,
                    onObtainiumTextChange = onObtainiumTextChange,
                    onImportObtainium = onImportObtainium,
                    onImportObtainiumUri = onImportObtainiumUri,
                )
            }
            state.message?.let { msg ->
                item {
                    Text(
                        text = formatSourcesMessage(msg),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    TextButton(onClick = onClearMessage) { Text(stringResource(R.string.sources_dismiss)) }
                }
            }
        }
    }
}

@Composable
private fun formatSourcesMessage(code: String): String {
    exactMessageRes(code)?.let { return stringResource(it) }
    return formatPrefixedMessage(code) ?: code
}

private fun exactMessageRes(code: String): Int? = EXACT_MESSAGE_RES[code]

private val EXACT_MESSAGE_RES = mapOf(
    "invalid_repo" to R.string.sources_msg_invalid_repo,
    "repo_added" to R.string.sources_msg_repo_added,
    "repo_uri_filled" to R.string.sources_msg_repo_uri_filled,
    "repo_http_warn" to R.string.sources_msg_repo_http_warn,
    "mirrors_saved" to R.string.sources_msg_mirrors_saved,
    "token_saved" to R.string.sources_msg_token_saved,
    "token_cleared" to R.string.sources_msg_token_cleared,
    "obtainium_empty" to R.string.sources_msg_obtainium_empty,
    "obtainium_too_short" to R.string.sources_msg_obtainium_too_short,
    "obtainium_no_apps" to R.string.sources_msg_obtainium_no_apps,
    "obtainium_file_read_failed" to R.string.sources_msg_obtainium_file_read_failed,
    "index_oom" to R.string.sources_msg_index_oom,
    "share_unrecognized" to R.string.sources_msg_share_unrecognized,
)

@Composable
private fun formatPrefixedMessage(code: String): String? = when {
    code.startsWith("package_page:") ->
        stringResource(R.string.sources_msg_package_page, code.removePrefix("package_page:"))

    code.startsWith("github_added:") ->
        stringResource(R.string.sources_msg_github_added, code.removePrefix("github_added:"))

    code.startsWith("github_exists:") ->
        stringResource(R.string.sources_msg_github_exists, code.removePrefix("github_exists:"))

    code.startsWith("obtainium:") ->
        stringResource(R.string.sources_msg_obtainium_imported, code.substringAfter(':'))

    code.startsWith("sync_ok:") ->
        stringResource(R.string.sources_msg_sync_ok, code.substringAfter(':'))

    code.startsWith("sync_fail:") -> {
        val parts = code.split(':', limit = 3)
        stringResource(
            R.string.sources_msg_sync_fail,
            parts.getOrNull(1) ?: "?",
            humanizeSyncDetail(parts.getOrNull(2).orEmpty()),
        )
    }

    code.startsWith("sync_partial:") -> {
        val parts = code.split(':', limit = 4)
        stringResource(
            R.string.sources_msg_sync_partial,
            parts.getOrNull(1) ?: "?",
            parts.getOrNull(2) ?: "?",
            humanizeSyncDetail(parts.getOrNull(3).orEmpty()),
        )
    }

    else -> null
}

@Composable
private fun humanizeSyncDetail(detail: String): String = when {
    detail.contains("index_oom") -> stringResource(R.string.sources_msg_index_oom)
    else -> detail
}

private fun LazyListScope.sourcesWatchlist(entries: List<WatchlistEntry>, onUnwatch: (String) -> Unit) {
    item {
        Text(
            text = stringResource(R.string.sources_watchlist_section),
            style = MaterialTheme.typography.titleMedium,
        )
    }
    if (entries.isEmpty()) {
        item {
            Text(
                text = stringResource(R.string.sources_watchlist_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        items(entries, key = { it.packageName }) { entry ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(entry.displayName, style = MaterialTheme.typography.titleSmall)
                    Text(entry.packageName, style = MaterialTheme.typography.bodySmall)
                    if (entry.hasUpdate && entry.availableVersionName != null) {
                        Text(
                            text = stringResource(
                                R.string.sources_watchlist_update,
                                entry.availableVersionName.orEmpty(),
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    entry.releaseNotes?.takeIf { it.isNotBlank() }?.let { notes ->
                        Text(
                            text = ReleaseNotesText.preview(notes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    OutlinedButton(onClick = { onUnwatch(entry.packageName) }) {
                        Text(stringResource(R.string.sources_watchlist_unwatch))
                    }
                }
            }
        }
    }
}

private fun LazyListScope.sourcesHeader(isSyncing: Boolean, onSyncAll: () -> Unit, onOpenAutoMatch: () -> Unit) {
    item {
        Text(
            text = stringResource(R.string.feature_sources_description),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    item {
        Button(onClick = onSyncAll, enabled = !isSyncing, modifier = Modifier.fillMaxWidth()) {
            if (isSyncing) {
                CircularProgressIndicator()
            } else {
                Text(stringResource(R.string.sources_sync_all))
            }
        }
    }
    item {
        Button(onClick = onOpenAutoMatch, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.sources_open_auto_match))
        }
    }
}

private fun LazyListScope.sourcesRepoList(repos: List<RepoUiModel>, onSaveRepoMirrors: (repoId: String, mirrorsRaw: String) -> Unit) {
    items(repos, key = { it.id }) { repo ->
        RepoCard(repo, onSaveRepoMirrors)
    }
}

@Composable
private fun RepoCard(repo: RepoUiModel, onSaveRepoMirrors: (repoId: String, mirrorsRaw: String) -> Unit) {
    var editingMirrors by remember(repo.id) { mutableStateOf(false) }
    var mirrorsDraft by remember(repo.id, repo.mirrorsCsv) {
        mutableStateOf(repo.mirrorsCsv.replace(',', '\n'))
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(repo.name, style = MaterialTheme.typography.titleMedium)
            Text(repo.address, style = MaterialTheme.typography.bodySmall)
            Text(
                stringResource(R.string.sources_repo_fingerprint, repo.fingerprint.take(16) + "…"),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                stringResource(
                    R.string.sources_repo_last_sync,
                    repo.lastSyncedLabel ?: stringResource(R.string.sources_never_synced),
                ),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                stringResource(R.string.sources_repo_mirrors_count, repo.mirrorCount),
                style = MaterialTheme.typography.bodySmall,
            )
            TextButton(onClick = { editingMirrors = true }) {
                Text(stringResource(R.string.sources_repo_edit_mirrors))
            }
        }
    }
    if (editingMirrors) {
        AlertDialog(
            onDismissRequest = { editingMirrors = false },
            title = { Text(stringResource(R.string.sources_repo_mirrors_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.sources_repo_mirrors_hint),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    OutlinedTextField(
                        value = mirrorsDraft,
                        onValueChange = { mirrorsDraft = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSaveRepoMirrors(repo.id, mirrorsDraft)
                        editingMirrors = false
                    },
                ) {
                    Text(stringResource(R.string.sources_repo_mirrors_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { editingMirrors = false }) {
                    Text(stringResource(R.string.sources_repo_mirrors_cancel))
                }
            },
        )
    }
}

@Composable
private fun AddRepoSection(
    state: SourcesUiState,
    onAddFieldsChange: (name: String?, address: String?, fingerprint: String?) -> Unit,
    onAddRepo: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.sources_add_repo), style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = state.addName,
            onValueChange = { onAddFieldsChange(it, null, null) },
            label = { Text(stringResource(R.string.sources_repo_name)) },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.addAddress,
            onValueChange = { onAddFieldsChange(null, it, null) },
            label = { Text(stringResource(R.string.sources_repo_url)) },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.addFingerprint,
            onValueChange = { onAddFieldsChange(null, null, it) },
            label = { Text(stringResource(R.string.sources_repo_fp)) },
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = onAddRepo, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.sources_add_repo_action))
        }
    }
}

@Composable
private fun GithubPatSection(
    state: SourcesUiState,
    onGithubTokenInputChange: (String) -> Unit,
    onSaveGithubToken: () -> Unit,
    onClearGithubToken: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.sources_github_pat), style = MaterialTheme.typography.titleMedium)
        Text(
            if (state.githubTokenSet) {
                stringResource(R.string.sources_github_pat_set)
            } else {
                stringResource(R.string.sources_github_pat_hint)
            },
            style = MaterialTheme.typography.bodySmall,
        )
        OutlinedTextField(
            value = state.githubTokenInput,
            onValueChange = onGithubTokenInputChange,
            label = { Text(stringResource(R.string.sources_github_pat_label)) },
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = onSaveGithubToken, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.sources_github_pat_save))
        }
        TextButton(onClick = onClearGithubToken) {
            Text(stringResource(R.string.sources_github_pat_clear))
        }
    }
}

@Composable
private fun ObtainiumSection(
    obtainiumText: String,
    onObtainiumTextChange: (String) -> Unit,
    onImportObtainium: () -> Unit,
    onImportObtainiumUri: (Uri) -> Unit,
) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onImportObtainiumUri(uri)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.sources_obtainium), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.sources_obtainium_hint),
            style = MaterialTheme.typography.bodySmall,
        )
        Button(
            onClick = { picker.launch(arrayOf("application/json", "text/*", "*/*")) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.sources_obtainium_pick_file))
        }
        OutlinedTextField(
            value = obtainiumText,
            onValueChange = onObtainiumTextChange,
            label = { Text(stringResource(R.string.sources_obtainium_paste)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
        )
        Button(onClick = onImportObtainium, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.sources_obtainium_import))
        }
    }
}

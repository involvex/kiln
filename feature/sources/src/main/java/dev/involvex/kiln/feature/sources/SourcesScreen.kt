package dev.involvex.kiln.feature.sources

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.involvex.kiln.core.designsystem.component.KilnScaffold
import dev.involvex.kiln.core.designsystem.kilnHiltViewModel
import dev.involvex.kiln.core.designsystem.scroll.ScrollToTopEffect
import dev.involvex.kiln.core.model.WatchlistEntry
import dev.involvex.kiln.feature.sources.github.StarredApkHint

@Composable
fun SourcesRoute(onOpenAutoMatch: () -> Unit, onOpenGithubTokenSettings: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel = kilnHiltViewModel<SourcesViewModel>()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.refreshGithubTokenStatus()
    }
    SourcesScreen(
        state = state,
        onSyncAll = viewModel::syncAll,
        onCancelSyncAll = viewModel::cancelSyncAll,
        onAddFieldsChange = viewModel::onAddFieldsChange,
        onAddRepo = viewModel::addRepo,
        onSaveRepoMirrors = viewModel::saveRepoMirrors,
        onMoveRepoPriority = viewModel::moveRepoPriority,
        onObtainiumTextChange = viewModel::onObtainiumTextChange,
        onImportObtainium = viewModel::importObtainium,
        onImportObtainiumUri = viewModel::importObtainiumFromUri,
        onOpenGithubTokenSettings = onOpenGithubTokenSettings,
        onOpenAutoMatch = onOpenAutoMatch,
        onLoadStarred = viewModel::loadStarredRepos,
        onToggleStarred = viewModel::toggleStarredSelection,
        onSelectStarredWithApk = viewModel::selectStarredWithApkHint,
        onClearStarredSelection = viewModel::clearStarredSelection,
        onAddSelectedStarred = viewModel::addSelectedStarredRepos,
        onUnwatch = viewModel::unwatch,
        onClearMessage = viewModel::clearMessage,
        modifier = modifier,
    )
}

@Composable
fun SourcesScreen(
    state: SourcesUiState,
    onSyncAll: () -> Unit,
    onCancelSyncAll: () -> Unit,
    onAddFieldsChange: (name: String?, address: String?, fingerprint: String?) -> Unit,
    onAddRepo: () -> Unit,
    onSaveRepoMirrors: (repoId: String, mirrorsRaw: String) -> Unit,
    onMoveRepoPriority: (repoId: String, delta: Int) -> Unit,
    onObtainiumTextChange: (String) -> Unit,
    onImportObtainium: () -> Unit,
    onImportObtainiumUri: (Uri) -> Unit,
    onOpenGithubTokenSettings: () -> Unit,
    onOpenAutoMatch: () -> Unit,
    onLoadStarred: () -> Unit,
    onToggleStarred: (String) -> Unit,
    onSelectStarredWithApk: () -> Unit,
    onClearStarredSelection: () -> Unit,
    onAddSelectedStarred: () -> Unit,
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
            sourcesHeader(state, onSyncAll, onCancelSyncAll, onOpenAutoMatch)
            sourcesWatchlist(state.watchlist, onUnwatch)
            sourcesRepoList(state.repos, onSaveRepoMirrors, onMoveRepoPriority)
            item { AddRepoSection(state, onAddFieldsChange, onAddRepo) }
            item { GithubPatStatusSection(state.githubTokenSet, onOpenGithubTokenSettings) }
            item {
                GithubStarsSection(
                    state = state,
                    onOpenGithubTokenSettings = onOpenGithubTokenSettings,
                    onLoadStarred = onLoadStarred,
                    onToggleStarred = onToggleStarred,
                    onSelectStarredWithApk = onSelectStarredWithApk,
                    onClearStarredSelection = onClearStarredSelection,
                    onAddSelectedStarred = onAddSelectedStarred,
                )
            }
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
    "sync_cancelled" to R.string.sources_msg_sync_cancelled,
    "github_pat_required" to R.string.sources_msg_github_pat_required,
    "stars_empty" to R.string.sources_msg_stars_empty,
    "stars_none_selected" to R.string.sources_msg_stars_none_selected,
    "stars_unauthorized" to R.string.sources_msg_stars_unauthorized,
    "stars_rate_limited" to R.string.sources_msg_stars_rate_limited,
)

@Composable
private fun formatPrefixedMessage(code: String): String? {
    formatStarsPrefixedMessage(code)?.let { return it }
    return formatSyncPrefixedMessage(code) ?: formatMiscPrefixedMessage(code)
}

@Composable
private fun formatStarsPrefixedMessage(code: String): String? = when {
    code.startsWith("stars_added:") -> {
        val parts = code.removePrefix("stars_added:").split(':')
        val added = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val existed = parts.getOrNull(1)?.toIntOrNull() ?: 0
        stringResource(R.string.sources_msg_stars_added, added, existed)
    }

    code.startsWith("stars_failed:") ->
        stringResource(R.string.sources_msg_stars_failed, code.removePrefix("stars_failed:"))

    else -> null
}

@Composable
private fun formatMiscPrefixedMessage(code: String): String? = when {
    code.startsWith("package_page:") ->
        stringResource(R.string.sources_msg_package_page, code.removePrefix("package_page:"))

    code.startsWith("github_added:") ->
        stringResource(R.string.sources_msg_github_added, code.removePrefix("github_added:"))

    code.startsWith("github_exists:") ->
        stringResource(R.string.sources_msg_github_exists, code.removePrefix("github_exists:"))

    code.startsWith("obtainium:") ->
        stringResource(R.string.sources_msg_obtainium_imported, code.substringAfter(':'))

    else -> null
}

@Composable
private fun formatSyncPrefixedMessage(code: String): String? = when {
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

private fun LazyListScope.sourcesHeader(
    state: SourcesUiState,
    onSyncAll: () -> Unit,
    onCancelSyncAll: () -> Unit,
    onOpenAutoMatch: () -> Unit,
) {
    item {
        Text(
            text = stringResource(R.string.feature_sources_description),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    item {
        Button(onClick = onSyncAll, enabled = !state.isSyncing, modifier = Modifier.fillMaxWidth()) {
            if (state.isSyncing) {
                CircularProgressIndicator()
            } else {
                Text(stringResource(R.string.sources_sync_all))
            }
        }
    }
    if (state.isSyncing) {
        item {
            Text(
                text = formatSyncProgress(state),
                style = MaterialTheme.typography.bodySmall,
            )
            TextButton(onClick = onCancelSyncAll) {
                Text(stringResource(R.string.sources_cancel_sync))
            }
        }
    }
    item {
        Button(onClick = onOpenAutoMatch, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.sources_open_auto_match))
        }
    }
}

@Composable
private fun formatSyncProgress(state: SourcesUiState): String {
    val label = state.syncProgressLabel
    val display = when (label) {
        null, "" -> stringResource(R.string.sources_syncing)
        "github" -> stringResource(R.string.sources_syncing_github)
        else -> label
    }
    return if (state.syncProgressTotal > 0) {
        stringResource(
            R.string.sources_syncing_progress,
            state.syncProgressCurrent,
            state.syncProgressTotal,
            display,
        )
    } else {
        display
    }
}

private fun LazyListScope.sourcesRepoList(
    repos: List<RepoUiModel>,
    onSaveRepoMirrors: (repoId: String, mirrorsRaw: String) -> Unit,
    onMoveRepoPriority: (repoId: String, delta: Int) -> Unit,
) {
    item {
        Text(
            text = stringResource(R.string.sources_repo_priority_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    items(repos, key = { it.id }) { repo ->
        val index = repos.indexOfFirst { it.id == repo.id }
        RepoCard(
            repo = repo,
            canMoveUp = index > 0,
            canMoveDown = index in 0 until repos.lastIndex,
            onSaveRepoMirrors = onSaveRepoMirrors,
            onMoveUp = { onMoveRepoPriority(repo.id, -1) },
            onMoveDown = { onMoveRepoPriority(repo.id, 1) },
        )
    }
}

@Composable
private fun RepoCard(
    repo: RepoUiModel,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onSaveRepoMirrors: (repoId: String, mirrorsRaw: String) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
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
                stringResource(R.string.sources_repo_priority, repo.priority),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                stringResource(R.string.sources_repo_mirrors_count, repo.mirrorCount),
                style = MaterialTheme.typography.bodySmall,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onMoveUp, enabled = canMoveUp) {
                    Text(stringResource(R.string.sources_repo_move_up))
                }
                OutlinedButton(onClick = onMoveDown, enabled = canMoveDown) {
                    Text(stringResource(R.string.sources_repo_move_down))
                }
                TextButton(onClick = { editingMirrors = true }) {
                    Text(stringResource(R.string.sources_repo_edit_mirrors))
                }
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
private fun GithubPatStatusSection(tokenSet: Boolean, onOpenGithubTokenSettings: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.sources_github_pat), style = MaterialTheme.typography.titleMedium)
        Text(
            if (tokenSet) {
                stringResource(R.string.sources_github_pat_set)
            } else {
                stringResource(R.string.sources_github_pat_hint)
            },
            style = MaterialTheme.typography.bodySmall,
        )
        OutlinedButton(onClick = onOpenGithubTokenSettings, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.sources_github_pat_open_settings))
        }
    }
}

@Composable
private fun GithubStarsSection(
    state: SourcesUiState,
    onOpenGithubTokenSettings: () -> Unit,
    onLoadStarred: () -> Unit,
    onToggleStarred: (String) -> Unit,
    onSelectStarredWithApk: () -> Unit,
    onClearStarredSelection: () -> Unit,
    onAddSelectedStarred: () -> Unit,
) {
    val selectedCount = state.starredRepos.count { it.selected }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.sources_stars_title), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.sources_stars_hint),
            style = MaterialTheme.typography.bodySmall,
        )
        if (!state.githubTokenSet) {
            OutlinedButton(onClick = onOpenGithubTokenSettings, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.sources_github_pat_open_settings))
            }
        } else {
            Button(
                onClick = onLoadStarred,
                enabled = !state.starredLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.sources_stars_load))
            }
        }
        if (state.starredLoading) {
            CircularProgressIndicator()
        }
        if (state.starredScanningApk) {
            Text(
                stringResource(R.string.sources_stars_scanning),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (state.starredLoaded && state.starredRepos.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onSelectStarredWithApk) {
                    Text(stringResource(R.string.sources_stars_select_apk))
                }
                OutlinedButton(onClick = onClearStarredSelection) {
                    Text(stringResource(R.string.sources_stars_clear_selection))
                }
            }
            Button(
                onClick = onAddSelectedStarred,
                enabled = selectedCount > 0,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.sources_stars_add_selected, selectedCount))
            }
            state.starredRepos.forEach { row ->
                StarredRepoRow(row = row, onToggle = { onToggleStarred(row.ownerRepo) })
            }
        }
    }
}

@Composable
private fun StarredRepoRow(row: StarredRepoUiModel, onToggle: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Checkbox(
                checked = row.selected,
                onCheckedChange = { onToggle() },
                enabled = !row.alreadyManaged,
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(row.ownerRepo, style = MaterialTheme.typography.titleSmall)
                row.description?.takeIf { it.isNotBlank() }?.let { desc ->
                    Text(desc, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                }
                Text(
                    starredStatusLabel(row),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun starredStatusLabel(row: StarredRepoUiModel): String {
    val parts = buildList {
        if (row.alreadyManaged) add(stringResource(R.string.sources_stars_already_managed))
        if (row.fork) add(stringResource(R.string.sources_stars_fork))
        add(
            when (row.apkHint) {
                StarredApkHint.UNKNOWN -> stringResource(R.string.sources_stars_apk_unknown)
                StarredApkHint.HAS_APK -> stringResource(R.string.sources_stars_apk_yes)
                StarredApkHint.NO_APK -> stringResource(R.string.sources_stars_apk_no)
                StarredApkHint.CHECK_FAILED -> stringResource(R.string.sources_stars_apk_failed)
            },
        )
    }
    return parts.joinToString(" · ")
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

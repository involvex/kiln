package dev.involvex.kiln.feature.sources

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.involvex.kiln.core.designsystem.kilnHiltViewModel
import dev.involvex.kiln.core.designsystem.scroll.ScrollToTopEffect

@Composable
fun SourcesReposRoute(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel = kilnHiltViewModel<SourcesViewModel>()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SourcesReposScreen(
        state = state,
        onBack = onBack,
        onAddFieldsChange = viewModel::onAddFieldsChange,
        onAddRepo = viewModel::addRepo,
        onSaveRepoMirrors = viewModel::saveRepoMirrors,
        onMoveRepoPriority = viewModel::moveRepoPriority,
        onClearMessage = viewModel::clearMessage,
        modifier = modifier,
    )
}

@Composable
fun SourcesReposScreen(
    state: SourcesUiState,
    onBack: () -> Unit,
    onAddFieldsChange: (name: String?, address: String?, fingerprint: String?) -> Unit,
    onAddRepo: () -> Unit,
    onSaveRepoMirrors: (repoId: String, mirrorsRaw: String) -> Unit,
    onMoveRepoPriority: (repoId: String, delta: Int) -> Unit,
    onClearMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    ScrollToTopEffect(listState)
    SourcesCategoryScaffold(
        title = stringResource(R.string.sources_hub_repos),
        onBack = onBack,
        modifier = modifier,
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.sources_repo_priority_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(state.repos, key = { it.id }) { repo ->
                val index = state.repos.indexOfFirst { it.id == repo.id }
                RepoCard(
                    repo = repo,
                    canMoveUp = index > 0,
                    canMoveDown = index in 0 until state.repos.lastIndex,
                    onSaveRepoMirrors = onSaveRepoMirrors,
                    onMoveUp = { onMoveRepoPriority(repo.id, -1) },
                    onMoveDown = { onMoveRepoPriority(repo.id, 1) },
                )
            }
            item {
                AddRepoSection(state, onAddFieldsChange, onAddRepo)
            }
            state.message?.let { msg ->
                item {
                    SourcesMessageBanner(message = msg, onClearMessage = onClearMessage)
                }
            }
        }
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

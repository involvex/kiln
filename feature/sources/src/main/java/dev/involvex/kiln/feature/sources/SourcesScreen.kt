package dev.involvex.kiln.feature.sources

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.involvex.kiln.core.designsystem.component.KilnScaffold

@Composable
fun SourcesRoute(onOpenAutoMatch: () -> Unit, modifier: Modifier = Modifier, viewModel: SourcesViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SourcesScreen(
        state = state,
        onSyncAll = viewModel::syncAll,
        onAddFieldsChange = viewModel::onAddFieldsChange,
        onAddRepo = viewModel::addRepo,
        onObtainiumTextChange = viewModel::onObtainiumTextChange,
        onImportObtainium = viewModel::importObtainium,
        onGithubTokenInputChange = viewModel::onGithubTokenInputChange,
        onSaveGithubToken = viewModel::saveGithubToken,
        onClearGithubToken = viewModel::clearGithubToken,
        onOpenAutoMatch = onOpenAutoMatch,
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
    onObtainiumTextChange: (String) -> Unit,
    onImportObtainium: () -> Unit,
    onGithubTokenInputChange: (String) -> Unit,
    onSaveGithubToken: () -> Unit,
    onClearGithubToken: () -> Unit,
    onOpenAutoMatch: () -> Unit,
    onClearMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    KilnScaffold(title = stringResource(R.string.feature_sources_title), modifier = modifier) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            sourcesHeader(state.isSyncing, onSyncAll, onOpenAutoMatch)
            sourcesRepoList(state.repos)
            item { AddRepoSection(state, onAddFieldsChange, onAddRepo) }
            item { GithubPatSection(state, onGithubTokenInputChange, onSaveGithubToken, onClearGithubToken) }
            item { ObtainiumSection(state.obtainiumText, onObtainiumTextChange, onImportObtainium) }
            state.message?.let { msg ->
                item {
                    Text(msg, color = MaterialTheme.colorScheme.primary)
                    TextButton(onClick = onClearMessage) { Text(stringResource(R.string.sources_dismiss)) }
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

private fun LazyListScope.sourcesRepoList(repos: List<RepoUiModel>) {
    items(repos, key = { it.id }) { repo ->
        RepoCard(repo)
    }
}

@Composable
private fun RepoCard(repo: RepoUiModel) {
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
        }
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
private fun ObtainiumSection(obtainiumText: String, onObtainiumTextChange: (String) -> Unit, onImportObtainium: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.sources_obtainium), style = MaterialTheme.typography.titleMedium)
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

package dev.involvex.kiln.feature.sources

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.involvex.kiln.core.designsystem.kilnHiltViewModel
import dev.involvex.kiln.feature.sources.github.StarredApkHint

@Composable
fun SourcesStarsRoute(onBack: () -> Unit, onOpenGithubTokenSettings: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel = kilnHiltViewModel<SourcesViewModel>()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SourcesStarsScreen(
        state = state,
        onBack = onBack,
        onOpenGithubTokenSettings = onOpenGithubTokenSettings,
        onLoadStarred = viewModel::loadStarredRepos,
        onToggleStarred = viewModel::toggleStarredSelection,
        onSelectStarredWithApk = viewModel::selectStarredWithApkHint,
        onClearStarredSelection = viewModel::clearStarredSelection,
        onAddSelectedStarred = viewModel::addSelectedStarredRepos,
        onClearMessage = viewModel::clearMessage,
        modifier = modifier,
    )
}

@Composable
fun SourcesStarsScreen(
    state: SourcesUiState,
    onBack: () -> Unit,
    onOpenGithubTokenSettings: () -> Unit,
    onLoadStarred: () -> Unit,
    onToggleStarred: (String) -> Unit,
    onSelectStarredWithApk: () -> Unit,
    onClearStarredSelection: () -> Unit,
    onAddSelectedStarred: () -> Unit,
    onClearMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val visibleRepos = filterStarredRepos(state.starredRepos, searchQuery)
    val selectedCount = state.starredRepos.count { it.selected }

    SourcesCategoryScaffold(
        title = stringResource(R.string.sources_hub_stars),
        onBack = onBack,
        modifier = modifier,
    ) { padding ->
        SourcesScrollColumn(padding = padding) {
            Text(
                stringResource(R.string.sources_stars_hint),
                style = MaterialTheme.typography.bodySmall,
            )
            GithubPatStatusSection(tokenSet = state.githubTokenSet)
            StarsTokenActions(
                tokenSet = state.githubTokenSet,
                loading = state.starredLoading,
                onOpenGithubTokenSettings = onOpenGithubTokenSettings,
                onLoadStarred = onLoadStarred,
            )
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
                StarredReposList(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    visibleRepos = visibleRepos,
                    totalCount = state.starredRepos.size,
                    selectedCount = selectedCount,
                    onToggleStarred = onToggleStarred,
                    onSelectStarredWithApk = onSelectStarredWithApk,
                    onClearStarredSelection = onClearStarredSelection,
                    onAddSelectedStarred = onAddSelectedStarred,
                )
            }
            state.message?.let { msg ->
                SourcesMessageBanner(message = msg, onClearMessage = onClearMessage)
            }
        }
    }
}

@Composable
private fun StarsTokenActions(tokenSet: Boolean, loading: Boolean, onOpenGithubTokenSettings: () -> Unit, onLoadStarred: () -> Unit) {
    OutlinedButton(onClick = onOpenGithubTokenSettings, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.sources_github_pat_open_settings))
    }
    if (tokenSet) {
        Button(
            onClick = onLoadStarred,
            enabled = !loading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.sources_stars_load))
        }
    }
}

@Composable
private fun StarredReposList(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    visibleRepos: List<StarredRepoUiModel>,
    totalCount: Int,
    selectedCount: Int,
    onToggleStarred: (String) -> Unit,
    onSelectStarredWithApk: () -> Unit,
    onClearStarredSelection: () -> Unit,
    onAddSelectedStarred: () -> Unit,
) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text(stringResource(R.string.sources_stars_search_hint)) },
        trailingIcon = {
            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { onSearchQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.sources_stars_search_clear),
                    )
                }
            }
        },
    )
    if (searchQuery.isNotBlank()) {
        Text(
            text = stringResource(R.string.sources_stars_search_count, visibleRepos.size, totalCount),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
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
    visibleRepos.forEach { row ->
        StarredRepoRow(row = row, onToggle = { onToggleStarred(row.ownerRepo) })
    }
}

@Composable
private fun GithubPatStatusSection(tokenSet: Boolean) {
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

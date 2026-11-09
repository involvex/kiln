package dev.involvex.kiln.feature.sources.discovery

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.involvex.kiln.core.designsystem.component.KilnScaffold
import dev.involvex.kiln.core.designsystem.scroll.ScrollToTopEffect
import dev.involvex.kiln.feature.sources.R

@Composable
fun DiscoveryRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenCatalogDetail: ((String, String) -> Unit)? = null,
    embedded: Boolean = false,
    showTopBar: Boolean = true,
    viewModel: DiscoveryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DiscoveryScreen(
        state = state,
        onBack = onBack,
        embedded = embedded,
        showTopBar = showTopBar,
        onOpenCatalogDetail = onOpenCatalogDetail,
        onSourceTab = viewModel::setSourceTab,
        onQueryChange = viewModel::setQuery,
        onCategory = viewModel::setCategory,
        onRefreshCatalog = viewModel::refreshCatalog,
        onInstall = viewModel::install,
        onGithubQueryChange = viewModel::setGithubQuery,
        onLookupGithub = viewModel::lookupGithub,
        onLookupSavedGithub = viewModel::lookupSavedGithub,
        onInstallGithub = viewModel::installGithub,
        onClearMessage = viewModel::clearMessage,
        modifier = modifier,
    )
}

@Composable
@Suppress("LongMethod", "LongParameterList")
fun DiscoveryScreen(
    state: DiscoveryUiState,
    onBack: () -> Unit,
    embedded: Boolean = false,
    showTopBar: Boolean = true,
    onOpenCatalogDetail: ((String, String) -> Unit)? = null,
    onSourceTab: (DiscoverySourceTab) -> Unit,
    onQueryChange: (String) -> Unit,
    onCategory: (String) -> Unit,
    onRefreshCatalog: () -> Unit,
    onInstall: (DiscoveryAppRow) -> Unit,
    onGithubQueryChange: (String) -> Unit,
    onLookupGithub: () -> Unit,
    onLookupSavedGithub: (DiscoveryGithubRow) -> Unit,
    onInstallGithub: (DiscoveryGithubRow) -> Unit,
    onClearMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    ScrollToTopEffect(listState)
    KilnScaffold(
        title = stringResource(R.string.discovery_title),
        modifier = modifier,
        showTopBar = showTopBar,
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefreshCatalog,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (!embedded) {
                    item {
                        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.discovery_back))
                        }
                    }
                }
                item {
                    SourceTabs(selected = state.sourceTab, onSelect = onSourceTab)
                }
                state.message?.let { msg ->
                    item {
                        Text(
                            text = formatDiscoveryMessage(msg),
                            color = if (state.messageIsError) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                        )
                        TextButton(onClick = onClearMessage) {
                            Text(stringResource(R.string.sources_dismiss))
                        }
                    }
                }
                when (state.sourceTab) {
                    DiscoverySourceTab.FDROID -> fdroidItems(
                        state = state,
                        onQueryChange = onQueryChange,
                        onCategory = onCategory,
                        onRefreshCatalog = onRefreshCatalog,
                        onInstall = onInstall,
                        onOpenCatalogDetail = onOpenCatalogDetail,
                    )

                    DiscoverySourceTab.GITHUB -> githubItems(
                        state = state,
                        onGithubQueryChange = onGithubQueryChange,
                        onLookupGithub = onLookupGithub,
                        onLookupSavedGithub = onLookupSavedGithub,
                        onInstallGithub = onInstallGithub,
                    )
                }
            }
        }
    }
}

private fun LazyListScope.fdroidItems(
    state: DiscoveryUiState,
    onQueryChange: (String) -> Unit,
    onCategory: (String) -> Unit,
    onRefreshCatalog: () -> Unit,
    onInstall: (DiscoveryAppRow) -> Unit,
    onOpenCatalogDetail: ((String, String) -> Unit)?,
) {
    item {
        Text(
            text = stringResource(R.string.discovery_description),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    item {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.discovery_catalog_count, state.packageCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = onRefreshCatalog,
                enabled = !state.isRefreshing,
            ) {
                Text(
                    text = if (state.isRefreshing) {
                        stringResource(R.string.discovery_refreshing)
                    } else {
                        stringResource(R.string.discovery_refresh_catalog)
                    },
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
    item {
        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(stringResource(R.string.discovery_search)) },
        )
    }
    item {
        CategoryChips(
            categories = state.categories,
            selected = state.selectedCategory,
            onSelect = onCategory,
        )
    }
    if (state.isSearching && state.results.isEmpty()) {
        item { CircularProgressIndicator() }
    }
    items(state.results, key = { "${it.repoId}:${it.packageName}" }) { row ->
        DiscoveryAppCard(
            row = row,
            onInstall = { onInstall(row) },
            onOpenDetail = onOpenCatalogDetail?.let { open ->
                { open(row.packageName, row.repoId) }
            },
        )
    }
    if (!state.isSearching && state.results.isEmpty() && state.packageCount == 0) {
        item {
            Text(
                text = stringResource(R.string.discovery_empty_catalog),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

private fun LazyListScope.githubItems(
    state: DiscoveryUiState,
    onGithubQueryChange: (String) -> Unit,
    onLookupGithub: () -> Unit,
    onLookupSavedGithub: (DiscoveryGithubRow) -> Unit,
    onInstallGithub: (DiscoveryGithubRow) -> Unit,
) {
    item {
        Text(
            text = stringResource(R.string.discovery_github_description),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    item {
        OutlinedTextField(
            value = state.githubQuery,
            onValueChange = onGithubQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(stringResource(R.string.discovery_github_query)) },
        )
    }
    item {
        Button(
            onClick = onLookupGithub,
            enabled = !state.isLookingUpGithub && state.githubQuery.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.discovery_github_lookup))
        }
    }
    if (state.isLookingUpGithub) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CircularProgressIndicator()
                Text(stringResource(R.string.discovery_github_looking_up))
            }
        }
    }
    items(state.githubResults, key = { "result:${it.ownerRepo}" }) { row ->
        GithubAppCard(
            row = row,
            onInstall = { onInstallGithub(row) },
            onLookup = null,
        )
    }
    if (state.savedGithub.isNotEmpty()) {
        item {
            Text(
                text = stringResource(R.string.discovery_github_saved),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        items(state.savedGithub, key = { "saved:${it.ownerRepo}:${it.packageName}" }) { row ->
            GithubAppCard(
                row = row,
                onInstall = { onInstallGithub(row) },
                onLookup = { onLookupSavedGithub(row) },
            )
        }
    } else if (!state.isLookingUpGithub && state.githubResults.isEmpty()) {
        item {
            Text(
                text = stringResource(R.string.discovery_github_empty),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun SourceTabs(selected: DiscoverySourceTab, onSelect: (DiscoverySourceTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selected == DiscoverySourceTab.FDROID,
            onClick = { onSelect(DiscoverySourceTab.FDROID) },
            label = { Text(stringResource(R.string.discovery_tab_fdroid)) },
        )
        FilterChip(
            selected = selected == DiscoverySourceTab.GITHUB,
            onClick = { onSelect(DiscoverySourceTab.GITHUB) },
            label = { Text(stringResource(R.string.discovery_tab_github)) },
        )
    }
}

@Composable
private fun CategoryChips(categories: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selected.isEmpty(),
            onClick = { onSelect("") },
            label = { Text(stringResource(R.string.discovery_category_all)) },
        )
        categories.forEach { category ->
            FilterChip(
                selected = selected == category,
                onClick = { onSelect(category) },
                label = { Text(category) },
            )
        }
    }
}

@Composable
private fun GithubAppCard(row: DiscoveryGithubRow, onInstall: () -> Unit, onLookup: (() -> Unit)?) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text = row.displayName, style = MaterialTheme.typography.titleMedium)
        Text(
            text = row.packageName,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val placeholder = stringResource(R.string.discovery_placeholder)
        Text(
            text = stringResource(
                R.string.discovery_github_meta,
                row.tagName ?: placeholder,
                row.assetName ?: placeholder,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (onLookup != null && row.downloadUrl == null) {
            OutlinedButton(onClick = onLookup, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.discovery_github_lookup))
            }
        }
        OutlinedButton(
            onClick = onInstall,
            enabled = row.downloadUrl != null || onLookup != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.discovery_install))
        }
    }
}

@Composable
private fun formatDiscoveryMessage(code: String): String = when {
    code.startsWith("catalog_ok:") ->
        stringResource(R.string.discovery_msg_catalog_ok, code.substringAfter(':'))

    code.startsWith("install_queued:") ->
        stringResource(R.string.discovery_msg_install_queued, code.substringAfter(':'))

    code.startsWith("github_lookup_ok:") ->
        stringResource(R.string.discovery_msg_github_lookup_ok, code.substringAfter(':'))

    code == "no_fdroid_repos" -> stringResource(R.string.discovery_msg_no_repos)

    code == "repo_missing" -> stringResource(R.string.discovery_msg_repo_missing)

    code == "release_missing" -> stringResource(R.string.discovery_msg_release_missing)

    code == "apk_url_invalid" -> stringResource(R.string.discovery_msg_apk_url)

    code == "github_query_invalid" -> stringResource(R.string.discovery_msg_github_query_invalid)

    code == "github_release_missing" -> stringResource(R.string.discovery_msg_github_release_missing)

    code == "no_matching_github_asset" -> stringResource(R.string.discovery_msg_github_no_apk)

    code == "index_oom" -> stringResource(R.string.sources_msg_index_oom)

    else -> code
}

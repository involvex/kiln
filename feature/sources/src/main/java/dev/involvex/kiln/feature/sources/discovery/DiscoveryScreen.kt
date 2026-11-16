package dev.involvex.kiln.feature.sources.discovery

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.involvex.kiln.core.designsystem.component.EmptyState
import dev.involvex.kiln.core.designsystem.component.KilnScaffold
import dev.involvex.kiln.core.designsystem.component.SectionHeader
import dev.involvex.kiln.core.designsystem.kilnHiltViewModel
import dev.involvex.kiln.core.designsystem.scroll.ScrollToTopEffect
import dev.involvex.kiln.feature.sources.R

@Composable
fun DiscoveryRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenCatalogDetail: ((String, String) -> Unit)? = null,
    embedded: Boolean = false,
    showTopBar: Boolean = true,
) {
    // Nav3 entry ViewModelStoreOwner can miss Hilt’s map; Activity owner resolves @HiltViewModel.
    val viewModel = kilnHiltViewModel<DiscoveryViewModel>()
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
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (!embedded) {
                    item {
                        OutlinedButton(
                            onClick = onBack,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                        ) {
                            Text(stringResource(R.string.discovery_back))
                        }
                    }
                }
                item {
                    SourceTabs(
                        selected = state.sourceTab,
                        onSelect = onSourceTab,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
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
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                        TextButton(
                            onClick = onClearMessage,
                            modifier = Modifier.padding(horizontal = 8.dp),
                        ) {
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
    fdroidHeader(state = state, onRefreshCatalog = onRefreshCatalog)
    if (state.showHomeShelves) {
        homeShelfItems(
            state = state,
            onInstall = onInstall,
            onOpenCatalogDetail = onOpenCatalogDetail,
            onCategory = onCategory,
        )
    }
    fdroidSearchSection(
        state = state,
        onQueryChange = onQueryChange,
        onCategory = onCategory,
        onInstall = onInstall,
        onOpenCatalogDetail = onOpenCatalogDetail,
    )
}

private fun LazyListScope.fdroidHeader(state: DiscoveryUiState, onRefreshCatalog: () -> Unit) {
    item {
        Text(
            text = stringResource(R.string.discovery_honesty),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
    item {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
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
}

private fun LazyListScope.fdroidSearchSection(
    state: DiscoveryUiState,
    onQueryChange: (String) -> Unit,
    onCategory: (String) -> Unit,
    onInstall: (DiscoveryAppRow) -> Unit,
    onOpenCatalogDetail: ((String, String) -> Unit)?,
) {
    item {
        SectionHeader(
            title = stringResource(R.string.discovery_search_section),
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
    item {
        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            singleLine = true,
            label = { Text(stringResource(R.string.discovery_search)) },
        )
    }
    if (!state.showHomeShelves) {
        item {
            CategoryChips(
                categories = state.categories,
                selected = state.selectedCategory,
                onSelect = onCategory,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        if (state.isSearching && state.results.isEmpty()) {
            item {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            }
        }
        items(state.results, key = { "${it.repoId}:${it.packageName}" }) { row ->
            DiscoveryAppCard(
                row = row,
                onInstall = { onInstall(row) },
                onOpenDetail = onOpenCatalogDetail?.let { open ->
                    { open(row.packageName, row.repoId) }
                },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
    if (state.packageCount == 0 && !state.shelvesLoading) {
        item {
            EmptyState(
                title = stringResource(R.string.discovery_empty_title),
                description = stringResource(R.string.discovery_empty_catalog),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp)
                    .padding(horizontal = 16.dp),
            )
        }
    }
}

private fun LazyListScope.homeShelfItems(
    state: DiscoveryUiState,
    onInstall: (DiscoveryAppRow) -> Unit,
    onOpenCatalogDetail: ((String, String) -> Unit)?,
    onCategory: (String) -> Unit,
) {
    if (state.shelvesLoading && state.shelves.isEmpty()) {
        item {
            CircularProgressIndicator(modifier = Modifier.padding(16.dp))
        }
    } else {
        items(state.shelves, key = { it.id.name }) { shelf ->
            DiscoverShelfSection(
                shelf = shelf,
                onOpenCatalogDetail = onOpenCatalogDetail,
                onInstall = onInstall,
            )
        }
    }
    if (state.categories.isNotEmpty()) {
        item {
            SectionHeader(
                title = stringResource(R.string.discovery_categories_section),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        item {
            CategoryChips(
                categories = state.categories,
                selected = state.selectedCategory,
                onSelect = onCategory,
                modifier = Modifier.padding(horizontal = 16.dp),
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
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
    item {
        OutlinedTextField(
            value = state.githubQuery,
            onValueChange = onGithubQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            singleLine = true,
            label = { Text(stringResource(R.string.discovery_github_query)) },
        )
    }
    item {
        Button(
            onClick = onLookupGithub,
            enabled = !state.isLookingUpGithub && state.githubQuery.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Text(stringResource(R.string.discovery_github_lookup))
        }
    }
    if (state.isLookingUpGithub) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 16.dp),
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
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
    if (state.savedGithub.isNotEmpty()) {
        item {
            SectionHeader(
                title = stringResource(R.string.discovery_github_saved),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        items(state.savedGithub, key = { "saved:${it.ownerRepo}:${it.packageName}" }) { row ->
            GithubAppCard(
                row = row,
                onInstall = { onInstallGithub(row) },
                onLookup = { onLookupSavedGithub(row) },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    } else if (!state.isLookingUpGithub && state.githubResults.isEmpty()) {
        item {
            EmptyState(
                title = stringResource(R.string.discovery_github_empty_title),
                description = stringResource(R.string.discovery_github_empty),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp)
                    .padding(horizontal = 16.dp),
            )
        }
    }
}

@Composable
private fun DiscoverShelfSection(
    shelf: TrendingShelf,
    onOpenCatalogDetail: ((String, String) -> Unit)?,
    onInstall: (DiscoveryAppRow) -> Unit,
) {
    SectionHeader(title = shelfTitle(shelf.id))
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        items(shelf.apps, key = { "${it.repoId}:${it.packageName}" }) { row ->
            DiscoveryAppCard(
                row = row,
                compact = true,
                modifier = Modifier.width(268.dp),
                onOpenDetail = onOpenCatalogDetail?.let { open ->
                    { open(row.packageName, row.repoId) }
                },
                onInstall = { onInstall(row) },
            )
        }
    }
}

@Composable
private fun shelfTitle(id: TrendingShelfId): String = when (id) {
    TrendingShelfId.RECENTLY_UPDATED -> stringResource(R.string.discovery_shelf_recent)
    TrendingShelfId.NEW_IN_CATALOG -> stringResource(R.string.discovery_shelf_new)
    TrendingShelfId.PRIVACY -> stringResource(R.string.trending_shelf_privacy)
    TrendingShelfId.INTERNET -> stringResource(R.string.trending_shelf_internet)
    TrendingShelfId.MEDIA -> stringResource(R.string.trending_shelf_media)
    TrendingShelfId.SYSTEM -> stringResource(R.string.trending_shelf_system)
    TrendingShelfId.AI -> stringResource(R.string.trending_shelf_ai)
}

@Composable
private fun SourceTabs(selected: DiscoverySourceTab, onSelect: (DiscoverySourceTab) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
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
private fun CategoryChips(categories: List<String>, selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
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
private fun GithubAppCard(row: DiscoveryGithubRow, onInstall: () -> Unit, onLookup: (() -> Unit)?, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = row.displayName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = row.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
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
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
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

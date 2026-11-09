package dev.involvex.kiln.feature.sources.discovery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.involvex.kiln.core.designsystem.component.KilnScaffold
import dev.involvex.kiln.core.designsystem.scroll.ScrollToTopEffect
import dev.involvex.kiln.feature.sources.R

@Composable
fun TrendingRoute(
    onOpenCatalogDetail: (String, String) -> Unit,
    modifier: Modifier = Modifier,
    showTopBar: Boolean = true,
    viewModel: TrendingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TrendingScreen(
        state = state,
        onOpenCatalogDetail = onOpenCatalogDetail,
        onInstall = viewModel::install,
        onRefreshCatalog = viewModel::refreshCatalog,
        onClearMessage = viewModel::clearMessage,
        showTopBar = showTopBar,
        modifier = modifier,
    )
}

@Composable
fun TrendingScreen(
    state: TrendingUiState,
    onOpenCatalogDetail: (String, String) -> Unit,
    onInstall: (DiscoveryAppRow) -> Unit,
    onRefreshCatalog: () -> Unit,
    onClearMessage: () -> Unit,
    showTopBar: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    ScrollToTopEffect(listState)
    KilnScaffold(
        title = stringResource(R.string.trending_title),
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
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    Text(
                        text = stringResource(R.string.trending_honesty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                state.message?.let { msg ->
                    item {
                        Text(
                            text = formatTrendingMessage(msg),
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
                if (state.isLoading && state.shelves.isEmpty()) {
                    item { CircularProgressIndicator() }
                } else if (state.shelves.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.trending_empty),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                } else {
                    items(state.shelves, key = { it.id.name }) { shelf ->
                        TrendingShelfSection(
                            shelf = shelf,
                            onOpenCatalogDetail = onOpenCatalogDetail,
                            onInstall = onInstall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrendingShelfSection(
    shelf: TrendingShelf,
    onOpenCatalogDetail: (String, String) -> Unit,
    onInstall: (DiscoveryAppRow) -> Unit,
) {
    ColumnSectionTitle(shelf.id)
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
    ) {
        items(shelf.apps, key = { "${it.repoId}:${it.packageName}" }) { row ->
            DiscoveryAppCard(
                row = row,
                modifier = Modifier.width(300.dp),
                onOpenDetail = { onOpenCatalogDetail(row.packageName, row.repoId) },
                onInstall = { onInstall(row) },
            )
        }
    }
}

@Composable
private fun ColumnSectionTitle(id: TrendingShelfId) {
    val title = when (id) {
        TrendingShelfId.PRIVACY -> stringResource(R.string.trending_shelf_privacy)
        TrendingShelfId.INTERNET -> stringResource(R.string.trending_shelf_internet)
        TrendingShelfId.MEDIA -> stringResource(R.string.trending_shelf_media)
        TrendingShelfId.SYSTEM -> stringResource(R.string.trending_shelf_system)
        TrendingShelfId.AI -> stringResource(R.string.trending_shelf_ai)
    }
    Text(text = title, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun formatTrendingMessage(code: String): String = when {
    code.startsWith("install_queued:") ->
        stringResource(R.string.discovery_msg_install_queued, code.substringAfter(':'))

    code.startsWith("catalog_ok:") ->
        stringResource(R.string.discovery_msg_catalog_ok, code.substringAfter(':'))

    else -> code
}

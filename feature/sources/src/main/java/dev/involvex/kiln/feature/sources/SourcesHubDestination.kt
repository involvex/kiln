package dev.involvex.kiln.feature.sources

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.involvex.kiln.core.designsystem.component.KilnScaffold
import dev.involvex.kiln.core.designsystem.scroll.ScrollToTopEffect

data class SourcesHubDestination(val titleRes: Int, val subtitleRes: Int, val onClick: () -> Unit)

fun sourcesHubDestinations(
    onOpenRepos: () -> Unit,
    onOpenWatchlist: () -> Unit,
    onOpenStars: () -> Unit,
    onOpenImport: () -> Unit,
    onOpenAutoMatch: () -> Unit,
): List<SourcesHubDestination> = listOf(
    SourcesHubDestination(R.string.sources_hub_repos, R.string.sources_hub_repos_sub, onOpenRepos),
    SourcesHubDestination(R.string.sources_hub_watchlist, R.string.sources_hub_watchlist_sub, onOpenWatchlist),
    SourcesHubDestination(R.string.sources_hub_stars, R.string.sources_hub_stars_sub, onOpenStars),
    SourcesHubDestination(R.string.sources_hub_import, R.string.sources_hub_import_sub, onOpenImport),
    SourcesHubDestination(R.string.sources_hub_match, R.string.sources_hub_match_sub, onOpenAutoMatch),
)

@Composable
internal fun SourcesCategoryScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit,
) {
    KilnScaffold(
        title = title,
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.sources_category_back),
                )
            }
        },
        content = content,
    )
}

@Composable
internal fun SourcesScrollColumn(
    padding: PaddingValues,
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val scrollState = rememberScrollState()
    ScrollToTopEffect(scrollState)
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
internal fun SourcesHubRow(title: String, subtitle: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

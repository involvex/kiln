package dev.involvex.kiln.feature.sources.discovery

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.involvex.kiln.feature.sources.R

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiscoveryAppCard(
    row: DiscoveryAppRow,
    onInstall: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenDetail: (() -> Unit)? = null,
    compact: Boolean = false,
) {
    val context = LocalContext.current
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = onOpenDetail != null) { onOpenDetail?.invoke() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (compact) 10.dp else 12.dp),
            horizontalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            DiscoveryListIcon(
                name = row.name,
                iconUrl = row.iconUrl,
                sizeDp = if (compact) 40 else 48,
            )
            DiscoveryAppCardContent(
                row = row,
                onInstall = onInstall,
                onOpenSource = { url ->
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                },
                compact = compact,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DiscoveryAppCardContent(
    row: DiscoveryAppRow,
    onInstall: () -> Unit,
    onOpenSource: (String) -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = row.name,
            style = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
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
        row.summary?.takeIf { it.isNotBlank() }?.let { summary ->
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = if (compact) 2 else 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (row.categories.isNotEmpty()) {
            DiscoveryCategoryRow(
                categories = row.categories,
                limit = if (compact) 2 else 4,
            )
        }
        if (!compact) {
            row.sourceCodeUrl?.takeIf { it.isNotBlank() }?.let { url ->
                TextButton(onClick = { onOpenSource(url) }) {
                    Text(stringResource(R.string.catalog_detail_source_code))
                }
            }
        }
        if (compact) {
            TextButton(
                onClick = onInstall,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.discovery_install))
            }
        } else {
            OutlinedButton(
                onClick = onInstall,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.discovery_install))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DiscoveryCategoryRow(categories: List<String>, limit: Int = 4) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        categories.take(limit).forEach { category ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Text(
                    text = category,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
private fun DiscoveryListIcon(name: String, iconUrl: String?, sizeDp: Int = 48) {
    val size = sizeDp.dp
    if (iconUrl != null) {
        AsyncImage(
            model = iconUrl,
            contentDescription = null,
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(10.dp)),
            contentScale = ContentScale.Crop,
        )
    } else {
        Surface(
            modifier = Modifier.size(size),
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Text(
                text = name.firstOrNull()?.uppercase() ?: "?",
                style = if (sizeDp <= 40) {
                    MaterialTheme.typography.titleMedium
                } else {
                    MaterialTheme.typography.titleLarge
                },
                modifier = Modifier.padding(if (sizeDp <= 40) 8.dp else 10.dp),
            )
        }
    }
}

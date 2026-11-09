package dev.involvex.kiln.feature.sources.discovery

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import dev.involvex.kiln.core.designsystem.component.KilnScaffold
import dev.involvex.kiln.feature.sources.R

@Composable
fun CatalogDetailRoute(
    packageName: String,
    repoId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CatalogDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(packageName, repoId) {
        viewModel.load(packageName, repoId)
    }
    CatalogDetailScreen(
        state = state,
        onBack = onBack,
        onInstall = viewModel::install,
        onClearMessage = viewModel::clearMessage,
        modifier = modifier,
    )
}

@Composable
fun CatalogDetailScreen(
    state: CatalogDetailUiState,
    onBack: () -> Unit,
    onInstall: () -> Unit,
    onClearMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val shareChooserTitle = stringResource(R.string.catalog_detail_share)
    val row = state.row
    KilnScaffold(
        title = stringResource(R.string.catalog_detail_title),
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.discovery_back),
                )
            }
        },
        actions = {
            if (row != null) {
                IconButton(
                    onClick = {
                        val body = CatalogShareLinks.shareBody(
                            name = row.name,
                            packageName = row.packageName,
                            repoId = row.repoId,
                            repoAddress = row.repoAddress,
                            sourceCodeUrl = row.sourceCodeUrl,
                            repoName = row.repoName,
                        )
                        val share = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, row.name)
                            putExtra(Intent.EXTRA_TEXT, body)
                        }
                        context.startActivity(
                            Intent.createChooser(share, shareChooserTitle),
                        )
                    },
                ) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = shareChooserTitle,
                    )
                }
            }
        },
    ) { padding ->
        when {
            state.isLoading -> CatalogDetailLoading(padding)

            row == null -> CatalogDetailError(padding, onBack)

            else -> CatalogDetailContent(
                row = row,
                message = state.message,
                messageIsError = state.messageIsError,
                padding = padding,
                onInstall = onInstall,
                onClearMessage = onClearMessage,
                onShare = {
                    val body = CatalogShareLinks.shareBody(
                        name = row.name,
                        packageName = row.packageName,
                        repoId = row.repoId,
                        repoAddress = row.repoAddress,
                        sourceCodeUrl = row.sourceCodeUrl,
                        repoName = row.repoName,
                    )
                    val share = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, row.name)
                        putExtra(Intent.EXTRA_TEXT, body)
                    }
                    context.startActivity(
                        Intent.createChooser(share, shareChooserTitle),
                    )
                },
            )
        }
    }
}

@Composable
private fun CatalogDetailLoading(padding: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun CatalogDetailError(padding: PaddingValues, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = stringResource(R.string.catalog_detail_missing))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.discovery_back))
        }
    }
}

@Composable
private fun CatalogDetailContent(
    row: DiscoveryAppRow,
    message: String?,
    messageIsError: Boolean,
    padding: PaddingValues,
    onInstall: () -> Unit,
    onClearMessage: () -> Unit,
    onShare: () -> Unit,
) {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (message != null) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatCatalogMessage(message),
                        modifier = Modifier.weight(1f),
                        color = if (messageIsError) {
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
        }
        item { CatalogHeroCard(row) }
        row.summary?.takeIf { it.isNotBlank() }?.let { summary ->
            item {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        if (row.categories.isNotEmpty()) {
            item { CategoryChipsReadOnly(categories = row.categories) }
        }
        if (row.antiFeatures.isNotEmpty()) {
            item { AntiFeatureChips(antiFeatures = row.antiFeatures) }
        }
        item { CatalogMetaCard(row) }
        item { CatalogLinksCard(row = row, context = context) }
        item { CatalogScreenshotsSection(row.phoneScreenshotUrls) }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FilledTonalButton(
                    onClick = onShare,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.catalog_detail_share))
                }
                Button(
                    onClick = onInstall,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.catalog_detail_install))
                }
            }
        }
    }
}

@Composable
private fun CatalogHeroCard(row: DiscoveryAppRow) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CatalogIcon(name = row.name, iconUrl = row.iconUrl, size = 88)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = row.name,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = row.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val version = row.versionName ?: stringResource(R.string.discovery_placeholder)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Text(
                        text = stringResource(R.string.catalog_detail_version, version),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun CatalogMetaCard(row: DiscoveryAppRow) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.catalog_detail_sources),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = row.repoName,
                style = MaterialTheme.typography.bodyLarge,
            )
            if (row.repoAddress.isNotBlank()) {
                Text(
                    text = row.repoAddress,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun CatalogLinksCard(row: DiscoveryAppRow, context: android.content.Context) {
    val storeUrl = CatalogShareLinks.storePageUrl(row.packageName, row.repoId, row.repoAddress)
    val sourceUrl = row.sourceCodeUrl?.takeIf { it.isNotBlank() }
    if (storeUrl == null && sourceUrl == null) return

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            Text(
                text = stringResource(R.string.catalog_detail_links),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            )
            storeUrl?.let { url ->
                TextButton(
                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) },
                ) {
                    Text(stringResource(R.string.catalog_detail_store_page))
                }
            }
            sourceUrl?.let { url ->
                TextButton(
                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) },
                ) {
                    Text(stringResource(R.string.catalog_detail_source_code))
                }
            }
        }
    }
}

@Composable
private fun CatalogScreenshotsSection(screenshotUrls: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.catalog_detail_screenshots),
            style = MaterialTheme.typography.titleMedium,
        )
        if (screenshotUrls.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (url in screenshotUrls) {
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        modifier = Modifier
                            .height(240.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit,
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.catalog_detail_no_screenshots),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CatalogIcon(name: String, iconUrl: String?, size: Int) {
    val shape = RoundedCornerShape(20.dp)
    if (iconUrl != null) {
        AsyncImage(
            model = iconUrl,
            contentDescription = null,
            modifier = Modifier
                .size(size.dp)
                .clip(shape),
            contentScale = ContentScale.Crop,
        )
    } else {
        Surface(
            modifier = Modifier.size(size.dp),
            shape = shape,
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(
                    text = name.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryChipsReadOnly(categories: List<String>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        categories.forEach { category ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Text(
                    text = category,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AntiFeatureChips(antiFeatures: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.catalog_detail_anti_features),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            antiFeatures.forEach { id ->
                val labelRes = AntiFeatureLabels.labelRes(id)
                val label = if (labelRes != null) stringResource(labelRes) else id
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }
    }
}

@Composable
private fun formatCatalogMessage(code: String): String = when {
    code.startsWith("install_queued:") ->
        stringResource(R.string.discovery_msg_install_queued, code.substringAfter(':'))

    code == "catalog_missing" -> stringResource(R.string.catalog_detail_missing)

    else -> code
}

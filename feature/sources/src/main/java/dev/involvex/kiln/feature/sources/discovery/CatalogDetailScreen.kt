package dev.involvex.kiln.feature.sources.discovery

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
    KilnScaffold(title = stringResource(R.string.catalog_detail_title), modifier = modifier) { padding ->
        when {
            state.isLoading -> {
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

            state.row == null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                ) {
                    Text(text = stringResource(R.string.catalog_detail_missing))
                    OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.discovery_back))
                    }
                }
            }

            else -> {
                val row = checkNotNull(state.row)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.discovery_back))
                        }
                    }
                    state.message?.let { msg ->
                        item {
                            Text(
                                text = formatCatalogMessage(msg),
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
                    item {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CatalogIcon(name = row.name, iconUrl = row.iconUrl)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = row.name, style = MaterialTheme.typography.headlineSmall)
                                Text(
                                    text = row.packageName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    item {
                        row.summary?.takeIf { it.isNotBlank() }?.let { summary ->
                            Text(text = summary, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    if (row.categories.isNotEmpty()) {
                        item {
                            CategoryChipsReadOnly(categories = row.categories)
                        }
                    }
                    item {
                        val version = row.versionName ?: stringResource(R.string.discovery_placeholder)
                        Text(
                            text = stringResource(R.string.catalog_detail_version, version),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    item {
                        Text(
                            text = stringResource(R.string.catalog_detail_sources),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = row.repoName,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        row.sourceCodeUrl?.takeIf { it.isNotBlank() }?.let { url ->
                            TextButton(
                                onClick = {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                },
                            ) {
                                Text(stringResource(R.string.catalog_detail_source_code))
                            }
                        }
                    }
                    if (row.phoneScreenshotUrls.isNotEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.catalog_detail_screenshots),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                for (url in row.phoneScreenshotUrls) {
                                    AsyncImage(
                                        model = url,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .height(240.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Fit,
                                    )
                                }
                            }
                        }
                    } else {
                        item {
                            Text(
                                text = stringResource(R.string.catalog_detail_no_screenshots),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    item {
                        Button(onClick = onInstall, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.catalog_detail_install))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CatalogIcon(name: String, iconUrl: String?) {
    if (iconUrl != null) {
        AsyncImage(
            model = iconUrl,
            contentDescription = null,
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop,
        )
    } else {
        Text(
            text = name.firstOrNull()?.uppercase() ?: "?",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier
                .size(64.dp)
                .padding(12.dp),
        )
    }
}

@Composable
private fun CategoryChipsReadOnly(categories: List<String>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        categories.forEach { category ->
            Text(
                text = category,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
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

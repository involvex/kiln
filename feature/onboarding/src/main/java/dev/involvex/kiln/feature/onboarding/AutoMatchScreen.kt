@file:Suppress("TooManyFunctions")

package dev.involvex.kiln.feature.onboarding

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.involvex.kiln.core.designsystem.component.KilnScaffold
import dev.involvex.kiln.core.designsystem.kilnHiltViewModel
import dev.involvex.kiln.core.model.MatchConfidence
import dev.involvex.kiln.feature.onboarding.match.AutoMatchConfidenceFilter
import dev.involvex.kiln.feature.onboarding.match.AutoMatchListFilter
import dev.involvex.kiln.feature.onboarding.match.MatchCandidate

@Composable
fun AutoMatchRoute(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel = kilnHiltViewModel<AutoMatchViewModel>()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AutoMatchScreen(
        state = state,
        onBack = onBack,
        onToggle = viewModel::toggle,
        onAcceptSelected = viewModel::acceptSelected,
        onAcceptAllSafe = viewModel::acceptAllSafe,
        onRefresh = viewModel::refresh,
        onOpenLink = viewModel::openLinkDialog,
        onDismissLink = viewModel::dismissLinkDialog,
        onLinkFdroid = viewModel::linkToFdroidRepo,
        onLinkGithub = viewModel::linkToGithub,
        onRequestWarningAccept = viewModel::requestWarningAccept,
        onDismissWarningConfirm = viewModel::dismissWarningConfirm,
        onConfirmWarningAccept = viewModel::confirmWarningAccept,
        onRequestUnlink = viewModel::requestUnlink,
        onDismissUnlinkConfirm = viewModel::dismissUnlinkConfirm,
        onConfirmUnlink = viewModel::confirmUnlink,
        onClearMessage = viewModel::clearMessage,
        onSearchQueryChange = viewModel::setSearchQuery,
        onConfidenceFilterChange = viewModel::setConfidenceFilter,
        onShowLinkedChange = viewModel::setShowLinked,
        onConsumeSnackbar = viewModel::consumeSnackbar,
        modifier = modifier,
    )
}

@Composable
@Suppress("LongParameterList")
fun AutoMatchScreen(
    state: AutoMatchUiState,
    onBack: () -> Unit,
    onToggle: (String) -> Unit,
    onAcceptSelected: () -> Unit,
    onAcceptAllSafe: () -> Unit,
    onRefresh: () -> Unit,
    onOpenLink: (MatchCandidate) -> Unit,
    onDismissLink: () -> Unit,
    onLinkFdroid: (packageName: String, repoId: String) -> Unit,
    onLinkGithub: (packageName: String, ownerRepo: String) -> Unit,
    onRequestWarningAccept: (MatchCandidate) -> Unit,
    onDismissWarningConfirm: () -> Unit,
    onConfirmWarningAccept: () -> Unit,
    onRequestUnlink: (MatchCandidate) -> Unit,
    onDismissUnlinkConfirm: () -> Unit,
    onConfirmUnlink: () -> Unit,
    onClearMessage: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onConfidenceFilterChange: (AutoMatchConfidenceFilter) -> Unit,
    onShowLinkedChange: (Boolean) -> Unit,
    onConsumeSnackbar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val linkedMessage = state.snackbarLinkedCount?.let { count ->
        stringResource(R.string.onboarding_accepted_count, count)
    }
    val unlinkedMessage = if (state.snackbarUnlinked) {
        stringResource(R.string.onboarding_unlinked)
    } else {
        null
    }
    LaunchedEffect(linkedMessage, unlinkedMessage) {
        val message = linkedMessage ?: unlinkedMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        onConsumeSnackbar()
    }
    KilnScaffold(
        title = stringResource(R.string.onboarding_auto_match_title),
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (state.isLoading) {
            LoadingPane(Modifier.padding(padding))
        } else {
            AutoMatchList(
                state = state,
                contentPadding = padding,
                onBack = onBack,
                onToggle = onToggle,
                onAcceptSelected = onAcceptSelected,
                onAcceptAllSafe = onAcceptAllSafe,
                onRefresh = onRefresh,
                onOpenLink = onOpenLink,
                onRequestWarningAccept = onRequestWarningAccept,
                onRequestUnlink = onRequestUnlink,
                onClearMessage = onClearMessage,
                onSearchQueryChange = onSearchQueryChange,
                onConfidenceFilterChange = onConfidenceFilterChange,
                onShowLinkedChange = onShowLinkedChange,
            )
        }
    }
    AutoMatchDialogs(
        state = state,
        onDismissLink = onDismissLink,
        onLinkFdroid = onLinkFdroid,
        onLinkGithub = onLinkGithub,
        onDismissWarningConfirm = onDismissWarningConfirm,
        onConfirmWarningAccept = onConfirmWarningAccept,
        onDismissUnlinkConfirm = onDismissUnlinkConfirm,
        onConfirmUnlink = onConfirmUnlink,
    )
}

@Composable
private fun LoadingPane(modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun AutoMatchDialogs(
    state: AutoMatchUiState,
    onDismissLink: () -> Unit,
    onLinkFdroid: (packageName: String, repoId: String) -> Unit,
    onLinkGithub: (packageName: String, ownerRepo: String) -> Unit,
    onDismissWarningConfirm: () -> Unit,
    onConfirmWarningAccept: () -> Unit,
    onDismissUnlinkConfirm: () -> Unit,
    onConfirmUnlink: () -> Unit,
) {
    state.linkTarget?.let { target ->
        LinkSourceDialog(
            candidate = target,
            enabledRepos = state.enabledRepos,
            savedGithubRepos = state.savedGithubRepos,
            onDismiss = onDismissLink,
            onLinkFdroid = { onLinkFdroid(target.packageName, it) },
            onLinkGithub = { onLinkGithub(target.packageName, it) },
        )
    }
    state.warningConfirmTarget?.let { target ->
        AlertDialog(
            onDismissRequest = onDismissWarningConfirm,
            title = { Text(stringResource(R.string.onboarding_warning_confirm_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.onboarding_warning_confirm_body,
                        target.label,
                        target.sourceLabel.ifBlank { target.repoId },
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = onConfirmWarningAccept) {
                    Text(stringResource(R.string.onboarding_warning_confirm_accept))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissWarningConfirm) {
                    Text(stringResource(R.string.onboarding_warning_confirm_cancel))
                }
            },
        )
    }
    state.unlinkConfirmTarget?.let { target ->
        AlertDialog(
            onDismissRequest = onDismissUnlinkConfirm,
            title = { Text(stringResource(R.string.onboarding_stop_tracking_title, target.label)) },
            text = { Text(stringResource(R.string.onboarding_stop_tracking_body)) },
            confirmButton = {
                TextButton(onClick = onConfirmUnlink) {
                    Text(stringResource(R.string.onboarding_stop_tracking_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissUnlinkConfirm) {
                    Text(stringResource(R.string.onboarding_stop_tracking_cancel))
                }
            },
        )
    }
}

private data class FilteredMatchGroups(
    val high: List<MatchCandidate>,
    val medium: List<MatchCandidate>,
    val warning: List<MatchCandidate>,
    val none: List<MatchCandidate>,
) {
    val isEmpty: Boolean
        get() = listOf(high, medium, warning, none).all { it.isEmpty() }
}

@Composable
private fun rememberFilteredGroups(state: AutoMatchUiState): FilteredMatchGroups {
    val high = AutoMatchListFilter.filter(
        state.high,
        state.searchQuery,
        state.confidenceFilter,
        state.showLinked,
    )
    val medium = AutoMatchListFilter.filter(
        state.medium,
        state.searchQuery,
        state.confidenceFilter,
        state.showLinked,
    )
    val warning = AutoMatchListFilter.filter(
        state.warning,
        state.searchQuery,
        state.confidenceFilter,
        state.showLinked,
    )
    val none = AutoMatchListFilter.filter(
        state.none,
        state.searchQuery,
        state.confidenceFilter,
        state.showLinked,
    )
    return FilteredMatchGroups(high, medium, warning, none)
}

@Composable
private fun AutoMatchList(
    state: AutoMatchUiState,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onToggle: (String) -> Unit,
    onAcceptSelected: () -> Unit,
    onAcceptAllSafe: () -> Unit,
    onRefresh: () -> Unit,
    onOpenLink: (MatchCandidate) -> Unit,
    onRequestWarningAccept: (MatchCandidate) -> Unit,
    onRequestUnlink: (MatchCandidate) -> Unit,
    onClearMessage: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onConfidenceFilterChange: (AutoMatchConfidenceFilter) -> Unit,
    onShowLinkedChange: (Boolean) -> Unit,
) {
    val groups = rememberFilteredGroups(state)
    val titleHigh = stringResource(R.string.onboarding_group_high)
    val titleMedium = stringResource(R.string.onboarding_group_medium)
    val titleWarning = stringResource(R.string.onboarding_group_warning)
    val titleNone = stringResource(R.string.onboarding_group_none)
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        autoMatchHeader(
            state = state,
            groupsEmpty = groups.isEmpty,
            onBack = onBack,
            onAcceptAllSafe = onAcceptAllSafe,
            onAcceptSelected = onAcceptSelected,
            onRefresh = onRefresh,
            onClearMessage = onClearMessage,
            onSearchQueryChange = onSearchQueryChange,
            onConfidenceFilterChange = onConfidenceFilterChange,
            onShowLinkedChange = onShowLinkedChange,
        )
        matchSection(
            titleHigh,
            groups.high,
            state.selected,
            true,
            onToggle,
            onOpenLink,
            onRequestWarningAccept,
            onRequestUnlink,
        )
        matchSection(
            titleMedium,
            groups.medium,
            state.selected,
            true,
            onToggle,
            onOpenLink,
            onRequestWarningAccept,
            onRequestUnlink,
        )
        matchSection(
            titleWarning,
            groups.warning,
            state.selected,
            false,
            onToggle,
            onOpenLink,
            onRequestWarningAccept,
            onRequestUnlink,
        )
        matchSection(
            titleNone,
            groups.none,
            state.selected,
            false,
            onToggle,
            onOpenLink,
            onRequestWarningAccept,
            onRequestUnlink,
        )
    }
}

private fun LazyListScope.autoMatchHeader(
    state: AutoMatchUiState,
    groupsEmpty: Boolean,
    onBack: () -> Unit,
    onAcceptAllSafe: () -> Unit,
    onAcceptSelected: () -> Unit,
    onRefresh: () -> Unit,
    onClearMessage: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onConfidenceFilterChange: (AutoMatchConfidenceFilter) -> Unit,
    onShowLinkedChange: (Boolean) -> Unit,
) {
    item {
        Button(onClick = onBack) { Text(stringResource(R.string.onboarding_back)) }
    }
    item {
        Text(stringResource(R.string.onboarding_auto_match_body), style = MaterialTheme.typography.bodyMedium)
    }
    if (state.indexEmpty) {
        item {
            Text(
                text = stringResource(R.string.onboarding_index_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
    item {
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(stringResource(R.string.onboarding_search_label)) },
            placeholder = { Text(stringResource(R.string.onboarding_search_hint)) },
        )
    }
    item {
        FilterChipsRow(
            confidenceFilter = state.confidenceFilter,
            showLinked = state.showLinked,
            onConfidenceFilterChange = onConfidenceFilterChange,
            onShowLinkedChange = onShowLinkedChange,
        )
    }
    item { ActionRow(onAcceptAllSafe, onAcceptSelected, onRefresh) }
    state.messageErrorKey?.let { key ->
        item {
            Text(
                text = errorMessage(key),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.clickable(onClick = onClearMessage),
            )
        }
    }
    if (groupsEmpty) {
        item {
            Text(
                text = stringResource(R.string.onboarding_filter_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterChipsRow(
    confidenceFilter: AutoMatchConfidenceFilter,
    showLinked: Boolean,
    onConfidenceFilterChange: (AutoMatchConfidenceFilter) -> Unit,
    onShowLinkedChange: (Boolean) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        FilterChip(
            selected = confidenceFilter == AutoMatchConfidenceFilter.ALL,
            onClick = { onConfidenceFilterChange(AutoMatchConfidenceFilter.ALL) },
            label = { Text(stringResource(R.string.onboarding_filter_all)) },
        )
        FilterChip(
            selected = confidenceFilter == AutoMatchConfidenceFilter.HIGH,
            onClick = { onConfidenceFilterChange(AutoMatchConfidenceFilter.HIGH) },
            label = { Text(stringResource(R.string.onboarding_chip_high)) },
        )
        FilterChip(
            selected = confidenceFilter == AutoMatchConfidenceFilter.MEDIUM,
            onClick = { onConfidenceFilterChange(AutoMatchConfidenceFilter.MEDIUM) },
            label = { Text(stringResource(R.string.onboarding_chip_medium)) },
        )
        FilterChip(
            selected = confidenceFilter == AutoMatchConfidenceFilter.WARNING,
            onClick = { onConfidenceFilterChange(AutoMatchConfidenceFilter.WARNING) },
            label = { Text(stringResource(R.string.onboarding_chip_warning)) },
        )
        FilterChip(
            selected = confidenceFilter == AutoMatchConfidenceFilter.NONE,
            onClick = { onConfidenceFilterChange(AutoMatchConfidenceFilter.NONE) },
            label = { Text(stringResource(R.string.onboarding_chip_none)) },
        )
        FilterChip(
            selected = showLinked,
            onClick = { onShowLinkedChange(!showLinked) },
            label = { Text(stringResource(R.string.onboarding_show_linked)) },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActionRow(onAcceptAllSafe: () -> Unit, onAcceptSelected: () -> Unit, onRefresh: () -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(onClick = onAcceptAllSafe) {
            Text(stringResource(R.string.onboarding_accept_all_safe), maxLines = 1)
        }
        Button(onClick = onAcceptSelected) {
            Text(stringResource(R.string.onboarding_accept_selected), maxLines = 1)
        }
        OutlinedButton(onClick = onRefresh) {
            Text(stringResource(R.string.onboarding_refresh), maxLines = 1)
        }
    }
}

@Composable
private fun errorMessage(key: String): String = when (key) {
    "bad_github" -> stringResource(R.string.onboarding_link_bad_github)
    "link_failed" -> stringResource(R.string.onboarding_link_failed)
    "unlink_failed" -> stringResource(R.string.onboarding_unlink_failed)
    else -> key
}

private fun LazyListScope.matchSection(
    title: String,
    items: List<MatchCandidate>,
    selected: Set<String>,
    selectable: Boolean,
    onToggle: (String) -> Unit,
    onOpenLink: (MatchCandidate) -> Unit,
    onRequestWarningAccept: (MatchCandidate) -> Unit,
    onRequestUnlink: (MatchCandidate) -> Unit,
) {
    if (items.isEmpty()) return
    item {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
    }
    items(
        items,
        key = { it.packageName + it.repoId + it.confidence.name + it.alreadyLinked + it.trackedSourceLabel },
    ) { candidate ->
        MatchRow(
            candidate = candidate,
            checked = candidate.packageName in selected,
            selectable = selectable && !candidate.alreadyLinked,
            onToggle = { onToggle(candidate.packageName) },
            onOpenLink = { onOpenLink(candidate) },
            onRequestWarningAccept = { onRequestWarningAccept(candidate) },
            onRequestUnlink = { onRequestUnlink(candidate) },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MatchRow(
    candidate: MatchCandidate,
    checked: Boolean,
    selectable: Boolean,
    onToggle: () -> Unit,
    onOpenLink: () -> Unit,
    onRequestWarningAccept: () -> Unit,
    onRequestUnlink: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (selectable) Modifier.clickable(onClick = onToggle) else Modifier)
                .padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (selectable) {
                Checkbox(checked = checked, onCheckedChange = { onToggle() })
            }
            PackageIcon(packageName = candidate.packageName)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(candidate.label, style = MaterialTheme.typography.bodyLarge)
                Text(candidate.packageName, style = MaterialTheme.typography.bodySmall)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    ConfidenceChip(candidate.confidence)
                    if (candidate.alreadyLinked) {
                        LinkedChip()
                    }
                    val suggestionLabel = when {
                        candidate.alreadyLinked && candidate.trackedSourceLabel.isNotBlank() ->
                            candidate.trackedSourceLabel

                        candidate.sourceLabel.isNotBlank() -> candidate.sourceLabel

                        else -> ""
                    }
                    if (suggestionLabel.isNotBlank()) {
                        SuggestionChip(onClick = {}, label = { Text(suggestionLabel) }, enabled = false)
                    }
                }
                Text(
                    text = matchStatusText(candidate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (candidate.alternateRepoLabels.isNotEmpty() && !candidate.alreadyLinked) {
                    Text(
                        text = stringResource(
                            R.string.onboarding_also_in_repos,
                            candidate.alternateRepoLabels.joinToString(),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                MatchRowActions(
                    candidate = candidate,
                    onOpenLink = onOpenLink,
                    onRequestWarningAccept = onRequestWarningAccept,
                    onRequestUnlink = onRequestUnlink,
                )
            }
        }
    }
}

@Composable
private fun matchStatusText(candidate: MatchCandidate): String = when {
    candidate.alreadyLinked && candidate.trackedSourceLabel.isNotBlank() ->
        stringResource(R.string.onboarding_tracking_from, candidate.trackedSourceLabel)

    candidate.alreadyLinked -> stringResource(R.string.onboarding_tracking_unspecified)

    else -> rationaleText(candidate.rationaleKey)
}

@Composable
private fun LinkedChip() {
    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.tertiaryContainer, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text = stringResource(R.string.onboarding_chip_linked),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun MatchRowActions(
    candidate: MatchCandidate,
    onOpenLink: () -> Unit,
    onRequestWarningAccept: () -> Unit,
    onRequestUnlink: () -> Unit,
) {
    if (candidate.alreadyLinked) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onOpenLink) {
                Text(stringResource(R.string.onboarding_change_source))
            }
            TextButton(onClick = onRequestUnlink) {
                Text(stringResource(R.string.onboarding_stop_tracking))
            }
        }
        return
    }
    when (candidate.confidence) {
        MatchConfidence.WARNING -> {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onRequestWarningAccept) {
                    Text(stringResource(R.string.onboarding_accept_warning))
                }
                TextButton(onClick = onOpenLink) {
                    Text(stringResource(R.string.onboarding_link_source))
                }
            }
        }

        MatchConfidence.NONE -> {
            TextButton(onClick = onOpenLink) {
                Text(stringResource(R.string.onboarding_link_source))
            }
        }

        MatchConfidence.MEDIUM -> {
            TextButton(onClick = onOpenLink) {
                Text(stringResource(R.string.onboarding_change_source))
            }
        }

        MatchConfidence.HIGH -> Unit
    }
}

@Composable
private fun ConfidenceChip(confidence: MatchConfidence) {
    val (label, container) = when (confidence) {
        MatchConfidence.HIGH -> stringResource(R.string.onboarding_chip_high) to MaterialTheme.colorScheme.primaryContainer
        MatchConfidence.MEDIUM -> stringResource(R.string.onboarding_chip_medium) to MaterialTheme.colorScheme.secondaryContainer
        MatchConfidence.WARNING -> stringResource(R.string.onboarding_chip_warning) to MaterialTheme.colorScheme.errorContainer
        MatchConfidence.NONE -> stringResource(R.string.onboarding_chip_none) to MaterialTheme.colorScheme.surfaceVariant
    }
    Box(
        modifier = Modifier
            .background(container, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun rationaleText(key: String): String = when (key) {
    MatchCandidate.Rationale.SIGNER_MATCH -> stringResource(R.string.onboarding_rationale_signer_match)
    MatchCandidate.Rationale.INDEX_NO_SIGNERS -> stringResource(R.string.onboarding_rationale_index_no_signers)
    MatchCandidate.Rationale.GITHUB_MAP -> stringResource(R.string.onboarding_rationale_github_map)
    MatchCandidate.Rationale.SIGNER_MISMATCH -> stringResource(R.string.onboarding_rationale_signer_mismatch)
    MatchCandidate.Rationale.NOT_FOUND -> stringResource(R.string.onboarding_rationale_not_found)
    else -> ""
}

@Composable
private fun PackageIcon(packageName: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap = remember(packageName) {
        runCatching {
            val drawable: Drawable = context.packageManager.getApplicationIcon(packageName)
            drawable.toBitmap().asImageBitmap()
        }.getOrNull()
    }
    if (bitmap != null) {
        Image(bitmap = bitmap, contentDescription = null, modifier = modifier.size(40.dp))
    } else {
        Box(
            modifier = modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
        )
    }
}

@Composable
private fun LinkSourceDialog(
    candidate: MatchCandidate,
    enabledRepos: List<LinkableRepo>,
    savedGithubRepos: List<String>,
    onDismiss: () -> Unit,
    onLinkFdroid: (repoId: String) -> Unit,
    onLinkGithub: (ownerRepo: String) -> Unit,
) {
    var githubInput by remember(candidate.packageName) {
        mutableStateOf(initialGithubInput(candidate))
    }
    val suggestedRepos = remember(candidate, enabledRepos) {
        suggestedFdroidRepos(candidate, enabledRepos)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.onboarding_link_dialog_title, candidate.label)) },
        text = {
            LinkSourceDialogBody(
                suggestedRepos = suggestedRepos,
                savedGithubRepos = savedGithubRepos,
                githubInput = githubInput,
                onGithubInputChange = { githubInput = it },
                onLinkFdroid = onLinkFdroid,
                onLinkGithub = onLinkGithub,
            )
        },
        confirmButton = {
            TextButton(onClick = { onLinkGithub(githubInput) }, enabled = githubInput.contains('/')) {
                Text(stringResource(R.string.onboarding_link_github_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.onboarding_link_cancel))
            }
        },
    )
}

private fun initialGithubInput(candidate: MatchCandidate): String = candidate.githubOwnerRepo
    ?: candidate.trackedSourceLabel
        .removePrefix("GitHub ")
        .takeIf { it.contains('/') }
    ?: candidate.sourceCodeSuggestion
        ?.substringAfter("github.com/")
        ?.trim('/')
        ?.takeWhile { it != '#' && it != '?' }
        .orEmpty()

private fun suggestedFdroidRepos(candidate: MatchCandidate, enabledRepos: List<LinkableRepo>): List<LinkableRepo> = buildList {
    if (candidate.repoId.isNotBlank() && !candidate.repoId.startsWith("github:")) {
        add(LinkableRepo(candidate.repoId, candidate.sourceLabel.ifBlank { candidate.repoId }))
    }
    candidate.alternateRepoIds.zip(candidate.alternateRepoLabels).forEach { (id, label) ->
        if (none { it.id == id }) add(LinkableRepo(id, label))
    }
    enabledRepos.forEach { repo ->
        if (none { it.id == repo.id }) add(repo)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LinkSourceDialogBody(
    suggestedRepos: List<LinkableRepo>,
    savedGithubRepos: List<String>,
    githubInput: String,
    onGithubInputChange: (String) -> Unit,
    onLinkFdroid: (repoId: String) -> Unit,
    onLinkGithub: (ownerRepo: String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.onboarding_link_dialog_body), style = MaterialTheme.typography.bodyMedium)
        Text(stringResource(R.string.onboarding_link_fdroid_section), style = MaterialTheme.typography.titleSmall)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            suggestedRepos.forEach { repo ->
                AssistChip(onClick = { onLinkFdroid(repo.id) }, label = { Text(repo.name) })
            }
        }
        if (suggestedRepos.isEmpty()) {
            Text(
                stringResource(R.string.onboarding_link_no_repos),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            stringResource(R.string.onboarding_link_saved_github_section),
            style = MaterialTheme.typography.titleSmall,
        )
        if (savedGithubRepos.isEmpty()) {
            Text(
                stringResource(R.string.onboarding_link_no_saved_github),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                savedGithubRepos.forEach { ownerRepo ->
                    AssistChip(onClick = { onLinkGithub(ownerRepo) }, label = { Text(ownerRepo) })
                }
            }
        }
        Text(stringResource(R.string.onboarding_link_github_section), style = MaterialTheme.typography.titleSmall)
        OutlinedTextField(
            value = githubInput,
            onValueChange = onGithubInputChange,
            label = { Text(stringResource(R.string.onboarding_link_github_hint)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Done,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

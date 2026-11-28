@file:Suppress("TooManyFunctions")

package dev.involvex.kiln.feature.storage

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.involvex.kiln.core.common.ByteSizeFormatter
import dev.involvex.kiln.core.designsystem.component.KilnScaffold
import dev.involvex.kiln.core.designsystem.kilnHiltViewModel
import dev.involvex.kiln.core.designsystem.scroll.ScrollToTopEffect
import dev.involvex.kiln.core.model.ApkLeftover
import dev.involvex.kiln.core.model.AppStorageStats
import dev.involvex.kiln.core.model.DuplicateFileGroup
import dev.involvex.kiln.core.model.OrphanStorageDir
import dev.involvex.kiln.core.model.StorageTreemapKind
import dev.involvex.kiln.core.model.StorageTreemapLayout
import dev.involvex.kiln.core.model.StorageTreemapLeaf
import dev.involvex.kiln.core.model.StorageTreemapRect

@Composable
fun StorageRoute(modifier: Modifier = Modifier, showTopBar: Boolean = true, onOpenPrivilegeSetup: () -> Unit = {}) {
    val viewModel = kilnHiltViewModel<StorageViewModel>()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val treePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri: Uri? ->
        if (uri != null) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, flags)
            }
            viewModel.scanLeftovers(uri)
        }
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissionAndLoad()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    StorageScreen(
        state = state,
        onOpenUsageAccess = {
            context.startActivity(viewModel.usageAccessIntent())
        },
        onOpenPrivilegeSetup = onOpenPrivilegeSetup,
        onRefresh = viewModel::load,
        onIncludeSystem = viewModel::setIncludeSystem,
        onQueryChange = viewModel::setQuery,
        onClearApp = viewModel::requestClearApp,
        onClearAll = viewModel::requestClearAllCaches,
        onConfirmClear = {
            val pending = state.pendingClear
            if (pending != null && !state.canClearPrivileged) {
                val pkg = pending.packageName
                if (pkg != null) {
                    context.startActivity(viewModel.appDetailsIntent(pkg))
                }
                viewModel.dismissClear()
            } else {
                viewModel.confirmClear()
            }
        },
        onDismissClear = viewModel::dismissClear,
        onOpenAppDetails = { pkg ->
            context.startActivity(viewModel.appDetailsIntent(pkg))
        },
        onPickLeftoverFolder = { treePicker.launch(null) },
        onToggleLeftover = viewModel::toggleLeftover,
        onRequestDeleteLeftovers = viewModel::requestDeleteSelectedLeftovers,
        onToggleLargeFile = viewModel::toggleLargeFile,
        onRequestDeleteLargeFiles = viewModel::requestDeleteSelectedLargeFiles,
        onToggleDuplicate = viewModel::toggleDuplicate,
        onRequestDeleteDuplicates = viewModel::requestDeleteSelectedDuplicates,
        onToggleTrash = viewModel::toggleTrash,
        onRequestRestoreTrash = viewModel::requestRestoreSelectedTrash,
        onRequestPurgeTrash = viewModel::requestPurgeSelectedTrash,
        onRefreshOrphans = viewModel::loadOrphans,
        onToggleOrphan = viewModel::toggleOrphan,
        onRequestDeleteOrphans = viewModel::requestDeleteSelectedOrphans,
        onConfirmDelete = viewModel::confirmDelete,
        onDismissDelete = viewModel::dismissDelete,
        onClearMessage = viewModel::clearMessage,
        showTopBar = showTopBar,
        modifier = modifier,
    )
}

@Composable
@Suppress("LongParameterList", "LongMethod")
fun StorageScreen(
    state: StorageUiState,
    onOpenUsageAccess: () -> Unit,
    onOpenPrivilegeSetup: () -> Unit,
    onRefresh: () -> Unit,
    onIncludeSystem: (Boolean) -> Unit,
    onQueryChange: (String) -> Unit,
    onClearApp: (AppStorageStats) -> Unit,
    onClearAll: () -> Unit,
    onConfirmClear: () -> Unit,
    onDismissClear: () -> Unit,
    onOpenAppDetails: (String) -> Unit,
    onPickLeftoverFolder: () -> Unit,
    onToggleLeftover: (String) -> Unit,
    onRequestDeleteLeftovers: () -> Unit,
    onToggleLargeFile: (String) -> Unit,
    onRequestDeleteLargeFiles: () -> Unit,
    onToggleDuplicate: (String) -> Unit,
    onRequestDeleteDuplicates: () -> Unit,
    onToggleTrash: (String) -> Unit,
    onRequestRestoreTrash: () -> Unit,
    onRequestPurgeTrash: () -> Unit,
    onRefreshOrphans: () -> Unit,
    onToggleOrphan: (OrphanStorageDir) -> Unit,
    onRequestDeleteOrphans: () -> Unit,
    onConfirmDelete: () -> Unit,
    onDismissDelete: () -> Unit,
    onClearMessage: () -> Unit,
    modifier: Modifier = Modifier,
    showTopBar: Boolean = true,
) {
    KilnScaffold(
        title = stringResource(R.string.feature_storage_title),
        modifier = modifier,
        showTopBar = showTopBar,
    ) { padding ->
        when (state.permission) {
            StoragePermissionState.UNKNOWN,
            StoragePermissionState.MISSING,
            -> UsageAccessGate(
                padding = padding,
                onOpenUsageAccess = onOpenUsageAccess,
            )

            StoragePermissionState.GRANTED -> StorageContent(
                padding = padding,
                state = state,
                onRefresh = onRefresh,
                onIncludeSystem = onIncludeSystem,
                onQueryChange = onQueryChange,
                onClearApp = onClearApp,
                onClearAll = onClearAll,
                onOpenAppDetails = onOpenAppDetails,
                onOpenPrivilegeSetup = onOpenPrivilegeSetup,
                onPickLeftoverFolder = onPickLeftoverFolder,
                onToggleLeftover = onToggleLeftover,
                onRequestDeleteLeftovers = onRequestDeleteLeftovers,
                onToggleLargeFile = onToggleLargeFile,
                onRequestDeleteLargeFiles = onRequestDeleteLargeFiles,
                onToggleDuplicate = onToggleDuplicate,
                onRequestDeleteDuplicates = onRequestDeleteDuplicates,
                onToggleTrash = onToggleTrash,
                onRequestRestoreTrash = onRequestRestoreTrash,
                onRequestPurgeTrash = onRequestPurgeTrash,
                onRefreshOrphans = onRefreshOrphans,
                onToggleOrphan = onToggleOrphan,
                onRequestDeleteOrphans = onRequestDeleteOrphans,
                onClearMessage = onClearMessage,
            )
        }
        state.pendingClear?.let { pending ->
            ClearCacheDialog(
                pending = pending,
                privileged = state.canClearPrivileged,
                clearing = state.clearing,
                onConfirm = onConfirmClear,
                onDismiss = onDismissClear,
            )
        }
        state.pendingDelete?.let { pending ->
            PendingDeleteDialog(
                pending = pending,
                deleting = state.deleting,
                onConfirm = onConfirmDelete,
                onDismiss = onDismissDelete,
            )
        }
    }
}

@Composable
private fun PendingDeleteDialog(pending: PendingStorageDelete, deleting: Boolean, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val body = pendingDeleteBody(pending)
    val confirmLabel = when (pending) {
        is PendingStorageDelete.TrashRestore -> stringResource(R.string.storage_restore_confirm)

        is PendingStorageDelete.TrashPurge,
        is PendingStorageDelete.Orphans,
        -> stringResource(R.string.storage_delete_confirm)

        else -> stringResource(R.string.storage_trash_confirm)
    }
    AlertDialog(
        onDismissRequest = { if (!deleting) onDismiss() },
        title = { Text(stringResource(R.string.storage_confirm_title)) },
        text = { Text(body) },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !deleting) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !deleting) {
                Text(stringResource(R.string.storage_cancel))
            }
        },
    )
}

@Composable
private fun pendingDeleteBody(pending: PendingStorageDelete): String = when (pending) {
    is PendingStorageDelete.Leftovers -> stringResource(
        R.string.storage_confirm_trash_leftovers,
        pending.items.size,
    )

    is PendingStorageDelete.LargeFiles -> stringResource(
        R.string.storage_confirm_trash_large,
        pending.items.size,
    )

    is PendingStorageDelete.Duplicates -> stringResource(
        R.string.storage_confirm_trash_duplicates,
        pending.items.size,
    )

    is PendingStorageDelete.TrashRestore -> stringResource(
        R.string.storage_confirm_trash_restore,
        pending.items.size,
    )

    is PendingStorageDelete.TrashPurge -> stringResource(
        R.string.storage_confirm_trash_purge,
        pending.items.size,
    )

    is PendingStorageDelete.Orphans -> stringResource(
        R.string.storage_confirm_delete_orphans,
        pending.items.size,
    )
}

@Composable
private fun UsageAccessGate(padding: PaddingValues, onOpenUsageAccess: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.storage_usage_required_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.storage_usage_required_body),
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(onClick = onOpenUsageAccess, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.storage_open_usage_access))
        }
    }
}

@Composable
@Suppress("LongParameterList", "LongMethod")
private fun StorageContent(
    padding: PaddingValues,
    state: StorageUiState,
    onRefresh: () -> Unit,
    onIncludeSystem: (Boolean) -> Unit,
    onQueryChange: (String) -> Unit,
    onClearApp: (AppStorageStats) -> Unit,
    onClearAll: () -> Unit,
    onOpenAppDetails: (String) -> Unit,
    onOpenPrivilegeSetup: () -> Unit,
    onPickLeftoverFolder: () -> Unit,
    onToggleLeftover: (String) -> Unit,
    onRequestDeleteLeftovers: () -> Unit,
    onToggleLargeFile: (String) -> Unit,
    onRequestDeleteLargeFiles: () -> Unit,
    onToggleDuplicate: (String) -> Unit,
    onRequestDeleteDuplicates: () -> Unit,
    onToggleTrash: (String) -> Unit,
    onRequestRestoreTrash: () -> Unit,
    onRequestPurgeTrash: () -> Unit,
    onRefreshOrphans: () -> Unit,
    onToggleOrphan: (OrphanStorageDir) -> Unit,
    onRequestDeleteOrphans: () -> Unit,
    onClearMessage: () -> Unit,
) {
    if (state.isLoading && state.apps.isEmpty()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator()
        }
        return
    }
    val listState = rememberLazyListState()
    ScrollToTopEffect(listState)
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { StorageTotals(state = state) }
        item {
            StorageTreemap(
                leaves = state.treemapLeaves(
                    leftoversLabel = stringResource(R.string.storage_treemap_leftovers),
                    orphansLabel = stringResource(R.string.storage_treemap_orphans),
                    largeFilesLabel = stringResource(R.string.storage_treemap_large_files),
                ),
                onOpenAppDetails = onOpenAppDetails,
            )
        }
        item { StorageToolbar(state, onRefresh, onIncludeSystem, onClearAll, onQueryChange) }
        item {
            LeftoversSection(
                state = state,
                onPickFolder = onPickLeftoverFolder,
                onToggle = onToggleLeftover,
                onDelete = onRequestDeleteLeftovers,
            )
        }
        item {
            LargeFilesSection(
                state = state,
                onToggle = onToggleLargeFile,
                onDelete = onRequestDeleteLargeFiles,
            )
        }
        item {
            DuplicatesSection(
                state = state,
                onToggle = onToggleDuplicate,
                onDelete = onRequestDeleteDuplicates,
            )
        }
        item {
            TrashSection(
                state = state,
                onToggle = onToggleTrash,
                onRestore = onRequestRestoreTrash,
                onPurge = onRequestPurgeTrash,
            )
        }
        item {
            OrphansSection(
                state = state,
                onOpenPrivilegeSetup = onOpenPrivilegeSetup,
                onRefresh = onRefreshOrphans,
                onToggle = onToggleOrphan,
                onDelete = onRequestDeleteOrphans,
            )
        }
        state.message?.let { message ->
            item {
                val text = storageMessageText(message)
                Text(text = text, style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = onClearMessage) {
                    Text(stringResource(R.string.storage_cancel))
                }
            }
        }
        state.error?.let { err ->
            item { Text(text = err, color = MaterialTheme.colorScheme.error) }
        }
        items(state.filteredApps, key = { it.packageName }) { app ->
            StorageAppRow(
                app = app,
                canClearPrivileged = state.canClearPrivileged,
                onClear = { onClearApp(app) },
                onOpenDetails = { onOpenAppDetails(app.packageName) },
            )
        }
    }
}

@Composable
private fun storageMessageText(message: String): String = when {
    message == "capability_missing" -> stringResource(R.string.storage_capability_missing)
    message == "no_tree" -> stringResource(R.string.storage_no_tree)
    message.startsWith("done:") -> resultFmt(R.string.storage_result_fmt, message.removePrefix("done:"))
    message.startsWith("trashed:") -> resultFmt(R.string.storage_result_trashed, message.removePrefix("trashed:"))
    message.startsWith("restored:") -> resultFmt(R.string.storage_result_restored, message.removePrefix("restored:"))
    message.startsWith("purged:") -> resultFmt(R.string.storage_result_purged, message.removePrefix("purged:"))
    else -> message
}

@Composable
private fun resultFmt(resId: Int, payload: String): String {
    val parts = payload.split(':')
    return stringResource(
        resId,
        parts.getOrNull(0)?.toIntOrNull() ?: 0,
        parts.getOrNull(1)?.toIntOrNull() ?: 0,
    )
}

private const val TREEMAP_GAP_PX = 1.5f
private const val TREEMAP_MIN_LABEL_WIDTH = 48f
private const val TREEMAP_MIN_LABEL_HEIGHT = 28f
private const val TREEMAP_LABEL_PAD_PX = 4f
private const val TREEMAP_LABEL_MARGIN_PX = 8f

@Composable
private fun StorageTreemap(leaves: List<StorageTreemapLeaf>, onOpenAppDetails: (String) -> Unit) {
    if (leaves.isEmpty()) return
    val colors = MaterialTheme.colorScheme
    val title = stringResource(R.string.storage_treemap_title)
    val hint = stringResource(R.string.storage_treemap_hint)
    val a11ySummary = leaves.take(6).joinToString { leaf ->
        "${leaf.label} ${ByteSizeFormatter.format(leaf.bytes)}"
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleSmall)
        Text(
            text = hint,
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
        )
        StorageTreemapCanvas(
            leaves = leaves,
            contentDescription = "$title. $a11ySummary",
            onOpenAppDetails = onOpenAppDetails,
        )
    }
}

@Composable
private fun StorageTreemapCanvas(leaves: List<StorageTreemapLeaf>, contentDescription: String, onOpenAppDetails: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val palette = remember(colors) { treemapFillColors(colors) }
    val onPalette = remember(colors) { treemapOnColors(colors) }
    val textMeasurer = rememberTextMeasurer()
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .semantics { this.contentDescription = contentDescription }
            .pointerInput(leaves) {
                detectTapGestures { offset ->
                    treemapAppPackageAt(leaves, size.width.toFloat(), size.height.toFloat(), offset.x, offset.y)
                        ?.let(onOpenAppDetails)
                }
            },
    ) {
        val rects = StorageTreemapLayout.layout(leaves, size.width, size.height)
        rects.forEachIndexed { index, rect ->
            drawTreemapCell(
                rect = rect,
                fill = palette[index % palette.size],
                onFill = onPalette[index % onPalette.size],
                textMeasurer = textMeasurer,
            )
        }
    }
}

private fun treemapFillColors(colors: ColorScheme) = listOf(
    colors.primary,
    colors.tertiary,
    colors.secondary,
    colors.primaryContainer,
    colors.tertiaryContainer,
    colors.secondaryContainer,
    colors.errorContainer,
    colors.surfaceVariant,
)

private fun treemapOnColors(colors: ColorScheme) = listOf(
    colors.onPrimary,
    colors.onTertiary,
    colors.onSecondary,
    colors.onPrimaryContainer,
    colors.onTertiaryContainer,
    colors.onSecondaryContainer,
    colors.onErrorContainer,
    colors.onSurfaceVariant,
)

private fun treemapAppPackageAt(leaves: List<StorageTreemapLeaf>, width: Float, height: Float, x: Float, y: Float): String? {
    val hit = StorageTreemapLayout.layout(leaves, width, height).firstOrNull { rect ->
        x >= rect.x && x < rect.x + rect.width && y >= rect.y && y < rect.y + rect.height
    } ?: return null
    if (hit.leaf.kind != StorageTreemapKind.APP) return null
    val pkg = hit.leaf.id.removePrefix("app:")
    return pkg.takeIf { it.isNotEmpty() }
}

private fun DrawScope.drawTreemapCell(rect: StorageTreemapRect, fill: Color, onFill: Color, textMeasurer: TextMeasurer) {
    val inset = TREEMAP_GAP_PX / 2f
    val w = (rect.width - TREEMAP_GAP_PX).coerceAtLeast(0f)
    val h = (rect.height - TREEMAP_GAP_PX).coerceAtLeast(0f)
    drawRect(
        color = fill,
        topLeft = Offset(rect.x + inset, rect.y + inset),
        size = Size(w, h),
    )
    if (w < TREEMAP_MIN_LABEL_WIDTH || h < TREEMAP_MIN_LABEL_HEIGHT) return
    val label = "${rect.leaf.label}\n${ByteSizeFormatter.format(rect.leaf.bytes)}"
    val layout = textMeasurer.measure(
        text = label,
        style = TextStyle(color = onFill, fontSize = 11.sp),
        maxLines = 2,
    )
    if (layout.size.width <= w - TREEMAP_LABEL_MARGIN_PX && layout.size.height <= h - TREEMAP_LABEL_MARGIN_PX) {
        drawText(
            textLayoutResult = layout,
            topLeft = Offset(rect.x + inset + TREEMAP_LABEL_PAD_PX, rect.y + inset + TREEMAP_LABEL_PAD_PX),
        )
    }
}

@Composable
private fun LeftoversSection(state: StorageUiState, onPickFolder: () -> Unit, onToggle: (String) -> Unit, onDelete: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.storage_leftovers_title), style = MaterialTheme.typography.titleSmall)
        Text(
            stringResource(R.string.storage_leftovers_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(onClick = onPickFolder, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.storage_leftovers_pick))
        }
        if (state.leftoversLoading) {
            CircularProgressIndicator()
        } else if (state.leftovers.isEmpty()) {
            Text(stringResource(R.string.storage_leftovers_empty), style = MaterialTheme.typography.bodySmall)
        } else {
            state.leftovers.forEach { item ->
                LeftoverRow(
                    item = item,
                    selected = item.id in state.selectedLeftoverIds,
                    onToggle = { onToggle(item.id) },
                )
            }
            OutlinedButton(
                onClick = onDelete,
                enabled = state.selectedLeftoverIds.isNotEmpty() && !state.deleting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.storage_leftovers_delete))
            }
        }
    }
}

@Composable
private fun LeftoverRow(item: ApkLeftover, selected: Boolean, onToggle: () -> Unit) {
    SelectableSizeRow(
        label = stringResource(
            R.string.storage_leftovers_item,
            item.displayName,
            ByteSizeFormatter.format(item.sizeBytes),
        ),
        selected = selected,
        onToggle = onToggle,
    )
}

@Composable
private fun LargeFilesSection(state: StorageUiState, onToggle: (String) -> Unit, onDelete: () -> Unit) {
    if (state.treeUri == null && state.largeFiles.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.storage_large_files_title), style = MaterialTheme.typography.titleSmall)
        when {
            state.leftoversLoading -> CircularProgressIndicator()

            state.largeFiles.isEmpty() -> Text(
                stringResource(R.string.storage_large_files_empty),
                style = MaterialTheme.typography.bodySmall,
            )

            else -> {
                state.largeFiles.forEach { item ->
                    SelectableSizeRow(
                        label = stringResource(
                            R.string.storage_leftovers_item,
                            item.relativePath,
                            ByteSizeFormatter.format(item.sizeBytes),
                        ),
                        selected = item.id in state.selectedLargeFileIds,
                        onToggle = { onToggle(item.id) },
                    )
                }
                OutlinedButton(
                    onClick = onDelete,
                    enabled = state.selectedLargeFileIds.isNotEmpty() && !state.deleting,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.storage_large_files_delete))
                }
            }
        }
    }
}

@Composable
private fun DuplicatesSection(state: StorageUiState, onToggle: (String) -> Unit, onDelete: () -> Unit) {
    if (state.treeUri == null && state.duplicates.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.storage_duplicates_title), style = MaterialTheme.typography.titleSmall)
        when {
            state.leftoversLoading -> Unit

            state.duplicates.isEmpty() -> Text(
                stringResource(R.string.storage_duplicates_empty),
                style = MaterialTheme.typography.bodySmall,
            )

            else -> {
                state.duplicates.forEach { group ->
                    DuplicateGroupBlock(group = group, selectedIds = state.selectedDuplicateIds, onToggle = onToggle)
                }
                OutlinedButton(
                    onClick = onDelete,
                    enabled = state.selectedDuplicateIds.isNotEmpty() && !state.deleting,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.storage_duplicates_delete))
                }
            }
        }
    }
}

@Composable
private fun DuplicateGroupBlock(group: DuplicateFileGroup, selectedIds: Set<String>, onToggle: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = stringResource(
                R.string.storage_duplicates_group,
                ByteSizeFormatter.format(group.sizeBytes),
                group.files.size,
                group.sha256.take(12),
            ),
            style = MaterialTheme.typography.labelMedium,
        )
        group.files.forEach { file ->
            SelectableSizeRow(
                label = stringResource(
                    R.string.storage_leftovers_item,
                    file.relativePath,
                    ByteSizeFormatter.format(file.sizeBytes),
                ),
                selected = file.id in selectedIds,
                onToggle = { onToggle(file.id) },
            )
        }
    }
}

@Composable
private fun TrashSection(state: StorageUiState, onToggle: (String) -> Unit, onRestore: () -> Unit, onPurge: () -> Unit) {
    if (state.treeUri == null && state.trash.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.storage_trash_title), style = MaterialTheme.typography.titleSmall)
        Text(
            stringResource(R.string.storage_trash_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        when {
            state.trash.isEmpty() -> Text(
                stringResource(R.string.storage_trash_empty),
                style = MaterialTheme.typography.bodySmall,
            )

            else -> {
                state.trash.forEach { entry ->
                    SelectableSizeRow(
                        label = stringResource(
                            R.string.storage_trash_item,
                            entry.displayName,
                            ByteSizeFormatter.format(entry.sizeBytes),
                        ),
                        selected = entry.id in state.selectedTrashIds,
                        onToggle = { onToggle(entry.id) },
                    )
                }
                OutlinedButton(
                    onClick = onRestore,
                    enabled = state.selectedTrashIds.isNotEmpty() && !state.deleting,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.storage_trash_restore))
                }
                OutlinedButton(
                    onClick = onPurge,
                    enabled = state.selectedTrashIds.isNotEmpty() && !state.deleting,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.storage_trash_purge))
                }
            }
        }
    }
}

@Composable
private fun SelectableSizeRow(label: String, selected: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = selected, onCheckedChange = { onToggle() })
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

@Composable
private fun OrphansSection(
    state: StorageUiState,
    onOpenPrivilegeSetup: () -> Unit,
    onRefresh: () -> Unit,
    onToggle: (OrphanStorageDir) -> Unit,
    onDelete: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.storage_orphans_title), style = MaterialTheme.typography.titleSmall)
        Text(
            stringResource(R.string.storage_orphans_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!state.canAccessAndroidData) {
            OutlinedButton(onClick = onOpenPrivilegeSetup, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.storage_orphans_privilege))
            }
        } else {
            OutlinedButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.storage_orphans_refresh))
            }
            when {
                state.orphansLoading -> CircularProgressIndicator()

                state.orphansError != null -> Text(
                    text = state.orphansError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )

                state.orphans.isEmpty() -> Text(
                    stringResource(R.string.storage_orphans_empty),
                    style = MaterialTheme.typography.bodySmall,
                )

                else -> {
                    state.orphans.forEach { orphan ->
                        val key = state.orphanKey(orphan)
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onToggle(orphan) },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = key in state.selectedOrphans,
                                onCheckedChange = { onToggle(orphan) },
                            )
                            Text(
                                text = stringResource(
                                    R.string.storage_orphans_item,
                                    orphan.root.pathKey,
                                    orphan.packageName,
                                    ByteSizeFormatter.format(orphan.sizeBytes),
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 4.dp),
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = onDelete,
                        enabled = state.selectedOrphans.isNotEmpty() && !state.deleting,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.storage_orphans_delete))
                    }
                }
            }
        }
    }
}

@Composable
private fun StorageToolbar(
    state: StorageUiState,
    onRefresh: () -> Unit,
    onIncludeSystem: (Boolean) -> Unit,
    onClearAll: () -> Unit,
    onQueryChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!state.canClearPrivileged) {
            Text(
                text = stringResource(R.string.storage_privilege_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(
                selected = !state.includeSystem,
                onClick = { onIncludeSystem(false) },
                label = { Text(stringResource(R.string.storage_filter_user)) },
            )
            FilterChip(
                selected = state.includeSystem,
                onClick = { onIncludeSystem(true) },
                label = { Text(stringResource(R.string.storage_filter_all)) },
            )
            OutlinedButton(onClick = onRefresh) {
                Text(stringResource(R.string.storage_refresh))
            }
        }
        OutlinedButton(
            onClick = onClearAll,
            enabled = state.totalCacheBytes > 0 && state.canClearPrivileged,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                stringResource(
                    R.string.storage_clear_all_caches,
                    ByteSizeFormatter.format(state.totalCacheBytes),
                ),
            )
        }
        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(stringResource(R.string.storage_search)) },
        )
    }
}

@Composable
private fun StorageTotals(state: StorageUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(
                R.string.storage_total_line,
                ByteSizeFormatter.format(state.totalBytes),
            ),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(
                R.string.storage_breakdown_line,
                ByteSizeFormatter.format(state.totalAppBytes),
                ByteSizeFormatter.format(state.totalDataBytes),
                ByteSizeFormatter.format(state.totalCacheBytes),
            ),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun StorageAppRow(app: AppStorageStats, canClearPrivileged: Boolean, onClear: () -> Unit, onOpenDetails: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenDetails)
            .padding(vertical = 6.dp),
    ) {
        Text(text = app.label, style = MaterialTheme.typography.titleSmall)
        Text(
            text = app.packageName,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(
                R.string.storage_app_breakdown,
                ByteSizeFormatter.format(app.totalBytes),
                ByteSizeFormatter.format(app.appBytes),
                ByteSizeFormatter.format(app.dataBytes),
                ByteSizeFormatter.format(app.cacheBytes),
            ),
            style = MaterialTheme.typography.bodySmall,
        )
        TextButton(onClick = onClear, enabled = app.cacheBytes > 0 || !canClearPrivileged) {
            Text(
                if (canClearPrivileged) {
                    stringResource(R.string.storage_clear_cache)
                } else {
                    stringResource(R.string.storage_open_app_info)
                },
            )
        }
    }
}

@Composable
private fun ClearCacheDialog(
    pending: PendingClearAction,
    privileged: Boolean,
    clearing: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val body = when {
        pending.packageName == null -> stringResource(
            R.string.storage_confirm_clear_all,
            ByteSizeFormatter.format(pending.cacheBytes),
        )

        privileged -> stringResource(
            R.string.storage_confirm_clear_app,
            pending.label,
            ByteSizeFormatter.format(pending.cacheBytes),
        )

        else -> stringResource(R.string.storage_confirm_open_settings, pending.label)
    }
    AlertDialog(
        onDismissRequest = { if (!clearing) onDismiss() },
        title = { Text(stringResource(R.string.storage_confirm_title)) },
        text = { Text(body) },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !clearing) {
                Text(stringResource(R.string.storage_confirm_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !clearing) {
                Text(stringResource(R.string.storage_cancel))
            }
        },
    )
}

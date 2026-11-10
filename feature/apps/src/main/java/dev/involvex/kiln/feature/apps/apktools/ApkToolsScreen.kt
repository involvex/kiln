package dev.involvex.kiln.feature.apps.apktools

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.involvex.kiln.core.designsystem.component.EmptyState
import dev.involvex.kiln.core.designsystem.component.KilnScaffold
import dev.involvex.kiln.core.designsystem.component.SectionHeader
import dev.involvex.kiln.core.designsystem.scroll.ScrollToTopEffect
import dev.involvex.kiln.core.model.ApkArchiveKind
import dev.involvex.kiln.core.model.ApkInspectReport
import dev.involvex.kiln.core.model.ApkSignatureCompareReport
import dev.involvex.kiln.feature.apps.R

private val ApkMimeTypes = arrayOf(
    "application/vnd.android.package-archive",
    "application/zip",
    "application/octet-stream",
    "*/*",
)

@Composable
fun ApkToolsRoute(modifier: Modifier = Modifier, showTopBar: Boolean = true, viewModel: ApkToolsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ApkToolsScreen(
        state = state,
        showTopBar = showTopBar,
        onMode = viewModel::setMode,
        onInspect = viewModel::inspect,
        onCompare = viewModel::compare,
        onCompareToInstalled = viewModel::compareToInstalled,
        onPackageQuery = viewModel::setComparePackageQuery,
        onClearMessage = viewModel::clearMessage,
        modifier = modifier,
    )
}

@Composable
@Suppress("LongParameterList")
fun ApkToolsScreen(
    state: ApkToolsUiState,
    onMode: (ApkToolsMode) -> Unit,
    onInspect: (Uri) -> Unit,
    onCompare: (Uri, Uri) -> Unit,
    onCompareToInstalled: (Uri) -> Unit,
    onPackageQuery: (String) -> Unit,
    onClearMessage: () -> Unit,
    showTopBar: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val scroll = rememberScrollState()
    ScrollToTopEffect(scroll)
    KilnScaffold(
        title = stringResource(R.string.apk_tools_title),
        modifier = modifier,
        showTopBar = showTopBar,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(scroll),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.apk_tools_description),
                style = MaterialTheme.typography.bodyMedium,
            )
            ModeChips(mode = state.mode, onMode = onMode)
            MessageBanner(state = state, onClearMessage = onClearMessage)
            if (state.isBusy) {
                CircularProgressIndicator()
            }
            when (state.mode) {
                ApkToolsMode.INSPECT -> InspectPane(
                    busy = state.isBusy,
                    report = state.inspect,
                    onInspect = onInspect,
                )

                ApkToolsMode.COMPARE -> ComparePane(
                    state = state,
                    onCompare = onCompare,
                    onCompareToInstalled = onCompareToInstalled,
                    onPackageQuery = onPackageQuery,
                )
            }
        }
    }
}

@Composable
private fun ModeChips(mode: ApkToolsMode, onMode: (ApkToolsMode) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = mode == ApkToolsMode.INSPECT,
            onClick = { onMode(ApkToolsMode.INSPECT) },
            label = { Text(stringResource(R.string.apk_tools_mode_inspect)) },
        )
        FilterChip(
            selected = mode == ApkToolsMode.COMPARE,
            onClick = { onMode(ApkToolsMode.COMPARE) },
            label = { Text(stringResource(R.string.apk_tools_mode_compare)) },
        )
    }
}

@Composable
private fun MessageBanner(state: ApkToolsUiState, onClearMessage: () -> Unit) {
    val code = state.message ?: return
    Text(
        text = formatApkToolsMessage(code),
        color = if (state.messageIsError) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.primary
        },
    )
    TextButton(onClick = onClearMessage) {
        Text(stringResource(R.string.apk_tools_dismiss))
    }
}

@Composable
private fun InspectPane(busy: Boolean, report: ApkInspectReport?, onInspect: (Uri) -> Unit) {
    val openInspect = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onInspect(uri)
    }
    OutlinedButton(
        onClick = { openInspect.launch(ApkMimeTypes) },
        enabled = !busy,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.apk_tools_pick_inspect))
    }
    if (report == null) {
        EmptyState(
            title = stringResource(R.string.apk_tools_empty_title),
            description = stringResource(R.string.apk_tools_empty_inspect),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 180.dp),
        )
    } else {
        InspectReport(report)
    }
}

@Composable
private fun ComparePane(
    state: ApkToolsUiState,
    onCompare: (Uri, Uri) -> Unit,
    onCompareToInstalled: (Uri) -> Unit,
    onPackageQuery: (String) -> Unit,
) {
    var pendingCompareLeft by remember { mutableStateOf<Uri?>(null) }
    val openCompareRight = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val left = pendingCompareLeft
        if (uri != null && left != null) {
            onCompare(left, uri)
            pendingCompareLeft = null
        }
    }
    val openCompareLeft = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            pendingCompareLeft = uri
            openCompareRight.launch(ApkMimeTypes)
        }
    }
    val openCompareInstalled = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onCompareToInstalled(uri)
    }
    OutlinedButton(
        onClick = {
            pendingCompareLeft = null
            openCompareLeft.launch(ApkMimeTypes)
        },
        enabled = !state.isBusy,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.apk_tools_pick_compare_left))
    }
    if (pendingCompareLeft != null) {
        OutlinedButton(
            onClick = { openCompareRight.launch(ApkMimeTypes) },
            enabled = !state.isBusy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.apk_tools_pick_compare_right))
        }
    }
    OutlinedTextField(
        value = state.comparePackageQuery,
        onValueChange = onPackageQuery,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text(stringResource(R.string.apk_tools_package_query)) },
    )
    OutlinedButton(
        onClick = { openCompareInstalled.launch(ApkMimeTypes) },
        enabled = !state.isBusy && state.comparePackageQuery.isNotBlank(),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.apk_tools_compare_installed))
    }
    val compare = state.compare
    if (compare == null) {
        EmptyState(
            title = stringResource(R.string.apk_tools_empty_title),
            description = stringResource(R.string.apk_tools_empty_compare),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 160.dp),
        )
    } else {
        CompareReport(compare)
    }
}

@Composable
private fun InspectReport(report: ApkInspectReport) {
    SectionHeader(title = stringResource(R.string.apk_tools_section_summary), modifier = Modifier.padding(0.dp))
    MetaLine(stringResource(R.string.apk_tools_field_file), report.displayName)
    MetaLine(stringResource(R.string.apk_tools_field_kind), kindLabel(report.kind))
    MetaLine(stringResource(R.string.apk_tools_field_package), report.packageName ?: stringResource(R.string.apk_tools_placeholder))
    MetaLine(
        stringResource(R.string.apk_tools_field_version),
        buildString {
            append(report.versionName ?: stringResource(R.string.apk_tools_placeholder))
            report.versionCode?.let { append(" ($it)") }
        },
    )
    MetaLine(
        stringResource(R.string.apk_tools_field_sdk),
        stringResource(
            R.string.apk_tools_sdk_value,
            report.minSdk?.toString() ?: "—",
            report.targetSdk?.toString() ?: "—",
        ),
    )
    SectionHeader(title = stringResource(R.string.apk_tools_section_signers), modifier = Modifier.padding(0.dp))
    if (report.signerSha256.isEmpty()) {
        Text(stringResource(R.string.apk_tools_no_signers), style = MaterialTheme.typography.bodySmall)
    } else {
        report.signerSha256.forEach { fp ->
            Text(fp, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
    SectionHeader(title = stringResource(R.string.apk_tools_section_permissions), modifier = Modifier.padding(0.dp))
    if (report.permissions.isEmpty()) {
        Text(stringResource(R.string.apk_tools_no_permissions), style = MaterialTheme.typography.bodySmall)
    } else {
        report.permissions.take(40).forEach { perm ->
            Text(perm, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (report.permissions.size > 40) {
            Text(
                stringResource(R.string.apk_tools_permissions_more, report.permissions.size - 40),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
    SectionHeader(title = stringResource(R.string.apk_tools_section_members), modifier = Modifier.padding(0.dp))
    report.baseApkMember?.let {
        MetaLine(stringResource(R.string.apk_tools_field_base), it)
    }
    report.members.take(60).forEach { member ->
        Text(member, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    if (report.members.size > 60) {
        Text(
            stringResource(R.string.apk_tools_members_more, report.members.size - 60),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun CompareReport(report: ApkSignatureCompareReport) {
    SectionHeader(title = stringResource(R.string.apk_tools_section_compare), modifier = Modifier.padding(0.dp))
    Text(
        text = if (report.matches) {
            stringResource(R.string.apk_tools_compare_match)
        } else {
            stringResource(R.string.apk_tools_compare_mismatch)
        },
        color = if (report.matches) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.error
        },
        style = MaterialTheme.typography.titleMedium,
    )
    MetaLine(stringResource(R.string.apk_tools_field_left), report.leftLabel)
    report.leftSigners.forEach {
        Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
    MetaLine(stringResource(R.string.apk_tools_field_right), report.rightLabel)
    report.rightSigners.forEach {
        Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun MetaLine(label: String, value: String) {
    Text(
        text = "$label: $value",
        style = MaterialTheme.typography.bodyMedium,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun kindLabel(kind: ApkArchiveKind): String = when (kind) {
    ApkArchiveKind.APK -> stringResource(R.string.apk_tools_kind_apk)
    ApkArchiveKind.APKS -> stringResource(R.string.apk_tools_kind_apks)
    ApkArchiveKind.XAPK -> stringResource(R.string.apk_tools_kind_xapk)
    ApkArchiveKind.UNKNOWN -> stringResource(R.string.apk_tools_kind_unknown)
}

@Composable
private fun formatApkToolsMessage(code: String): String = when (code) {
    "copy_failed" -> stringResource(R.string.apk_tools_msg_copy_failed)
    "unsupported_archive" -> stringResource(R.string.apk_tools_msg_unsupported)
    "apk_unreadable" -> stringResource(R.string.apk_tools_msg_apk_unreadable)
    "bundle_unreadable" -> stringResource(R.string.apk_tools_msg_bundle_unreadable)
    "bundle_no_apk" -> stringResource(R.string.apk_tools_msg_bundle_no_apk)
    "bundle_extract_failed" -> stringResource(R.string.apk_tools_msg_bundle_extract)
    "signer_unreadable" -> stringResource(R.string.apk_tools_msg_signer_unreadable)
    "package_not_installed" -> stringResource(R.string.apk_tools_msg_package_missing)
    "package_query_empty" -> stringResource(R.string.apk_tools_msg_package_empty)
    else -> code
}

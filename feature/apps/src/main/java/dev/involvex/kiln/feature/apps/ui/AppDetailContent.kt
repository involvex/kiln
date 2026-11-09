package dev.involvex.kiln.feature.apps.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.involvex.kiln.core.model.AppUpdateInfo
import dev.involvex.kiln.core.model.GitHubReleaseChannel
import dev.involvex.kiln.core.model.InstallJobState
import dev.involvex.kiln.core.model.InstallJobStatus
import dev.involvex.kiln.core.model.InstalledAppDetail
import dev.involvex.kiln.core.model.ManagedSourceLink
import dev.involvex.kiln.core.model.RollbackApkEntry
import dev.involvex.kiln.feature.apps.R
import dev.involvex.kiln.feature.apps.data.PlayStoreLinks
import dev.involvex.kiln.feature.apps.icon.PackageIconModel
import dev.involvex.kiln.feature.apps.util.formatApkSize
import java.text.DateFormat
import java.util.Date

@Composable
fun AppDetailContent(
    detail: InstalledAppDetail,
    installerLabel: String?,
    onCopied: () -> Unit,
    onExport: () -> Unit,
    availableUpdate: AppUpdateInfo? = null,
    installJob: InstallJobState? = null,
    ignoreUpdates: Boolean? = null,
    onIgnoreUpdatesChanged: (checkForUpdates: Boolean) -> Unit = {},
    managedLink: ManagedSourceLink? = null,
    onOpenLinkSource: () -> Unit = {},
    onGitHubChannelChange: (GitHubReleaseChannel) -> Unit = {},
    hasGitHubToken: Boolean = false,
    onInstallUpdate: () -> Unit = {},
    onOpenUnknownSources: () -> Unit = {},
    rollbackEntries: List<RollbackApkEntry> = emptyList(),
    canSilentDowngrade: Boolean = false,
    rollbackInProgress: Boolean = false,
    onRequestRollback: (RollbackApkEntry) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Header(detail)
        Text(
            text = stringResource(
                R.string.feature_apps_detail_version,
                detail.versionName ?: "—",
                detail.versionCode,
            ),
        )
        Text(
            text = stringResource(R.string.feature_apps_detail_sdk, detail.minSdk, detail.targetSdk),
        )
        Text(
            text = stringResource(R.string.feature_apps_detail_installed, formatDate(detail.firstInstallTime)),
        )
        Text(
            text = stringResource(R.string.feature_apps_detail_updated, formatDate(detail.lastUpdateTime)),
        )
        Text(
            text = stringResource(
                R.string.feature_apps_detail_installer,
                installerLabel ?: stringResource(R.string.feature_apps_detail_installer_unknown),
            ),
        )
        SignatureSection(detail = detail, onCopied = onCopied)
        UpdatePrefsSection(
            ignoreUpdates = ignoreUpdates,
            onIgnoreUpdatesChanged = onIgnoreUpdatesChanged,
        )
        ManagedSourceSection(
            managedLink = managedLink,
            onOpenLinkSource = onOpenLinkSource,
            onGitHubChannelChange = onGitHubChannelChange,
            hasGitHubToken = hasGitHubToken,
            showOpenInPlayStore = PlayStoreLinks.shouldOfferOpenInPlayStore(
                hasManagedLink = managedLink != null,
                installerPackage = detail.installerPackage,
            ),
            onOpenInPlayStore = {
                PlayStoreLinks.openListing(context, detail.packageName)
            },
        )
        PathsSection(detail = detail)
        PermissionsSection(detail = detail)
        if (!detail.canUninstall) {
            Text(
                text = stringResource(R.string.feature_apps_uninstall_disabled),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        UpdateSection(
            availableUpdate = availableUpdate,
            installJob = installJob,
            onInstallUpdate = onInstallUpdate,
            onOpenUnknownSources = onOpenUnknownSources,
        )
        RollbackSection(
            entries = rollbackEntries,
            canSilentDowngrade = canSilentDowngrade,
            rollbackInProgress = rollbackInProgress,
            onRequestRollback = onRequestRollback,
        )
        ActionButtons(
            detail = detail,
            context = context,
            onExport = onExport,
        )
    }
}

@Composable
private fun RollbackSection(
    entries: List<RollbackApkEntry>,
    canSilentDowngrade: Boolean,
    rollbackInProgress: Boolean,
    onRequestRollback: (RollbackApkEntry) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.feature_apps_rollback_section), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.feature_apps_rollback_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (rollbackInProgress) {
            Text(stringResource(R.string.feature_apps_rollback_busy))
        }
        if (entries.isEmpty()) {
            Text(
                stringResource(R.string.feature_apps_rollback_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            entries.forEach { entry ->
                OutlinedButton(
                    onClick = { onRequestRollback(entry) },
                    enabled = canSilentDowngrade && !rollbackInProgress,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        stringResource(
                            R.string.feature_apps_rollback_action,
                            entry.versionName,
                            entry.versionCode,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun ManagedSourceSection(
    managedLink: ManagedSourceLink?,
    onOpenLinkSource: () -> Unit,
    onGitHubChannelChange: (GitHubReleaseChannel) -> Unit,
    hasGitHubToken: Boolean,
    showOpenInPlayStore: Boolean,
    onOpenInPlayStore: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.feature_apps_managed_source_section), style = MaterialTheme.typography.titleMedium)
        if (managedLink == null) {
            Text(
                stringResource(R.string.feature_apps_managed_source_none),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (showOpenInPlayStore) {
                OutlinedButton(onClick = onOpenInPlayStore, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.feature_apps_open_in_play_store))
                }
            }
        } else {
            Text(
                stringResource(
                    R.string.feature_apps_managed_source_current,
                    managedLink.sourceId,
                    managedLink.confidence.name,
                ),
            )
            if (managedLink.pinned) {
                Text(
                    stringResource(R.string.feature_apps_managed_source_pinned),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!managedLink.githubOwnerRepo.isNullOrBlank()) {
                Text(
                    stringResource(R.string.feature_apps_github_channel_label),
                    style = MaterialTheme.typography.titleSmall,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    GitHubReleaseChannel.entries.forEach { channel ->
                        FilterChip(
                            selected = managedLink.githubChannel == channel,
                            onClick = { onGitHubChannelChange(channel) },
                            label = {
                                Text(
                                    stringResource(
                                        when (channel) {
                                            GitHubReleaseChannel.STABLE -> R.string.feature_apps_github_channel_stable
                                            GitHubReleaseChannel.PRE_RELEASE -> R.string.feature_apps_github_channel_pre_release
                                            GitHubReleaseChannel.NIGHTLY -> R.string.feature_apps_github_channel_nightly
                                        },
                                    ),
                                )
                            },
                        )
                    }
                }
                if (managedLink.githubChannel == GitHubReleaseChannel.NIGHTLY && !hasGitHubToken) {
                    Text(
                        stringResource(R.string.feature_apps_github_channel_nightly_needs_pat),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
        OutlinedButton(onClick = onOpenLinkSource, modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(
                    if (managedLink == null) {
                        R.string.feature_apps_link_source
                    } else {
                        R.string.feature_apps_change_source
                    },
                ),
            )
        }
    }
}

@Composable
private fun UpdatePrefsSection(ignoreUpdates: Boolean?, onIgnoreUpdatesChanged: (Boolean) -> Unit) {
    // Switch ON means "check for updates" (ignoreUpdates == false or not yet managed).
    val checking = ignoreUpdates != true
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.feature_apps_ignore_updates))
            Text(
                stringResource(R.string.feature_apps_ignore_updates_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checking,
            onCheckedChange = onIgnoreUpdatesChanged,
        )
    }
}

@Composable
private fun UpdateSection(
    availableUpdate: AppUpdateInfo?,
    installJob: InstallJobState?,
    onInstallUpdate: () -> Unit,
    onOpenUnknownSources: () -> Unit,
) {
    if (availableUpdate == null && installJob == null) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.feature_apps_update_section), style = MaterialTheme.typography.titleMedium)
        if (availableUpdate != null) {
            Text(
                stringResource(
                    R.string.feature_apps_update_available,
                    availableUpdate.availableVersionName,
                    availableUpdate.sourceLabel,
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (!availableUpdate.signatureMatches) {
                Text(
                    stringResource(R.string.feature_apps_update_signature_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            availableUpdate.releaseNotes?.takeIf { it.isNotBlank() }?.let { notes ->
                ReleaseNotesBlock(notes = notes)
            }
            val canInstall = availableUpdate.signatureMatches &&
                !availableUpdate.downloadUrl.isNullOrBlank() &&
                installJob?.status != InstallJobStatus.DOWNLOADING &&
                installJob?.status != InstallJobStatus.INSTALLING &&
                installJob?.status != InstallJobStatus.VERIFYING
            Button(onClick = onInstallUpdate, enabled = canInstall) {
                Text(stringResource(R.string.feature_apps_action_update))
            }
        }
        installJob?.let { job ->
            Text(
                stringResource(R.string.feature_apps_install_status, job.status.name, job.progressPercent),
                style = MaterialTheme.typography.bodySmall,
            )
            if (job.errorMessage == "install_permission_required") {
                TextButton(onClick = onOpenUnknownSources) {
                    Text(stringResource(R.string.feature_apps_open_unknown_sources))
                }
            } else if (job.errorMessage != null) {
                Text(
                    stringResource(R.string.feature_apps_install_error, job.errorMessage.orEmpty()),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun ReleaseNotesBlock(notes: String) {
    var expanded by remember(notes) { mutableStateOf(false) }
    val previewLimit = 600
    val showToggle = notes.length > previewLimit
    val body = if (expanded || !showToggle) notes else notes.take(previewLimit).trimEnd() + "…"
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            stringResource(R.string.feature_apps_update_release_notes),
            style = MaterialTheme.typography.titleSmall,
        )
        Text(body, style = MaterialTheme.typography.bodySmall)
        if (showToggle) {
            TextButton(onClick = { expanded = !expanded }) {
                Text(
                    stringResource(
                        if (expanded) {
                            R.string.feature_apps_update_release_notes_less
                        } else {
                            R.string.feature_apps_update_release_notes_more
                        },
                    ),
                )
            }
        }
    }
}

@Composable
private fun Header(detail: InstalledAppDetail) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = PackageIconModel(detail.packageName, detail.lastUpdateTime),
            contentDescription = null,
            modifier = Modifier.size(64.dp),
        )
        Column {
            Text(detail.label, style = MaterialTheme.typography.headlineSmall)
            Text(detail.packageName, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SignatureSection(detail: InstalledAppDetail, onCopied: () -> Unit) {
    val context = LocalContext.current
    Text(stringResource(R.string.feature_apps_detail_signature), style = MaterialTheme.typography.titleMedium)
    detail.signerSha256.forEach { fingerprint ->
        Text(fingerprint, style = MaterialTheme.typography.bodySmall)
    }
    if (detail.signerSha256.isNotEmpty()) {
        TextButton(
            onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("sha256", detail.signerSha256.first()))
                onCopied()
            },
        ) {
            Text(stringResource(R.string.feature_apps_detail_copy_signature))
        }
    }
}

@Composable
private fun PathsSection(detail: InstalledAppDetail) {
    Text(stringResource(R.string.feature_apps_detail_paths), style = MaterialTheme.typography.titleMedium)
    detail.apkPaths.forEach { path ->
        val size = formatApkSize(path.sizeBytes) ?: stringResource(R.string.feature_apps_size_unknown)
        val id = path.splitId ?: "base"
        Text("$id · $size\n${path.path}", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun PermissionsSection(detail: InstalledAppDetail) {
    Text(stringResource(R.string.feature_apps_detail_permissions), style = MaterialTheme.typography.titleMedium)
    detail.requestedPermissions.take(PERMISSION_PREVIEW_LIMIT).forEach { permission ->
        val status = if (permission.granted) {
            stringResource(R.string.feature_apps_permission_granted)
        } else {
            stringResource(R.string.feature_apps_permission_denied)
        }
        val dangerous = if (permission.isDangerous) {
            " · ${stringResource(R.string.feature_apps_permission_dangerous)}"
        } else {
            ""
        }
        Text("${permission.name}: $status$dangerous", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ActionButtons(detail: InstalledAppDetail, context: Context, onExport: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        if (detail.hasLaunchIntent) {
            Button(
                onClick = {
                    context.packageManager.getLaunchIntentForPackage(detail.packageName)?.let {
                        context.startActivity(it)
                    }
                },
            ) {
                Text(stringResource(R.string.feature_apps_action_launch))
            }
        }
        OutlinedButton(
            onClick = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", detail.packageName, null)
                }
                context.startActivity(intent)
            },
        ) {
            Text(stringResource(R.string.feature_apps_action_info))
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = {
                val intent = Intent(Intent.ACTION_DELETE).apply {
                    data = Uri.fromParts("package", detail.packageName, null)
                }
                context.startActivity(intent)
            },
            enabled = detail.canUninstall,
        ) {
            Text(stringResource(R.string.feature_apps_action_uninstall))
        }
        Button(onClick = onExport) {
            Text(stringResource(R.string.feature_apps_action_export))
        }
    }
}

private fun formatDate(epochMillis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(epochMillis))

private const val PERMISSION_PREVIEW_LIMIT = 40

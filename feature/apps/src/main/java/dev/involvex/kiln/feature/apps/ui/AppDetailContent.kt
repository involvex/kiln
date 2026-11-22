@file:Suppress("TooManyFunctions")

package dev.involvex.kiln.feature.apps.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.involvex.kiln.core.model.AppActivityInfo
import dev.involvex.kiln.core.model.AppUpdateInfo
import dev.involvex.kiln.core.model.GitHubReleaseChannel
import dev.involvex.kiln.core.model.InstallJobState
import dev.involvex.kiln.core.model.InstallJobStatus
import dev.involvex.kiln.core.model.InstalledAppDetail
import dev.involvex.kiln.core.model.ManagedSourceLink
import dev.involvex.kiln.core.model.MatchConfidence
import dev.involvex.kiln.core.model.RollbackApkEntry
import dev.involvex.kiln.feature.apps.R
import dev.involvex.kiln.feature.apps.data.PlayStoreLinks
import dev.involvex.kiln.feature.apps.icon.PackageIconModel
import dev.involvex.kiln.feature.apps.util.formatApkSize
import java.text.DateFormat
import java.util.Date
import kotlin.math.roundToInt

@Composable
@Suppress("LongMethod", "LongParameterList")
fun AppDetailContent(
    detail: InstalledAppDetail,
    installerLabel: String?,
    onCopied: () -> Unit,
    onExport: () -> Unit,
    availableUpdate: AppUpdateInfo? = null,
    installJob: InstallJobState? = null,
    ignoreUpdates: Boolean? = null,
    onIgnoreUpdatesChanged: (checkForUpdates: Boolean) -> Unit = {},
    updateCooldownDays: Int? = null,
    globalCooldownDays: Int = 0,
    onUpdateCooldownChanged: (days: Int?) -> Unit = {},
    managedLink: ManagedSourceLink? = null,
    onOpenLinkSource: () -> Unit = {},
    onStopTracking: () -> Unit = {},
    onGitHubChannelChange: (GitHubReleaseChannel) -> Unit = {},
    hasGitHubToken: Boolean = false,
    onInstallUpdate: () -> Unit = {},
    onRequestInstallDespiteSignature: () -> Unit = {},
    onOpenUnknownSources: () -> Unit = {},
    rollbackEntries: List<RollbackApkEntry> = emptyList(),
    canSilentDowngrade: Boolean = false,
    rollbackInProgress: Boolean = false,
    onRequestRollback: (RollbackApkEntry) -> Unit = {},
    activities: List<AppActivityInfo> = emptyList(),
    activitiesLoading: Boolean = false,
    activitiesQuery: String = "",
    onActivitiesQueryChange: (String) -> Unit = {},
    showActivitiesSearch: Boolean = false,
    onActivityMessage: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var activitiesExpanded by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HeroCard(detail = detail, hasUpdate = availableUpdate != null)
        PrimaryActions(
            detail = detail,
            context = context,
            availableUpdate = availableUpdate,
            installJob = installJob,
            onInstallUpdate = onInstallUpdate,
            onExport = onExport,
        )
        if (!detail.canUninstall) {
            Text(
                text = stringResource(R.string.feature_apps_uninstall_disabled),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        UpdateSection(
            availableUpdate = availableUpdate,
            installJob = installJob,
            onInstallUpdate = onInstallUpdate,
            onRequestInstallDespiteSignature = onRequestInstallDespiteSignature,
            onOpenUnknownSources = onOpenUnknownSources,
        )
        DetailSectionCard(title = stringResource(R.string.feature_apps_detail_section_updates)) {
            ManagedSourceSection(
                managedLink = managedLink,
                onOpenLinkSource = onOpenLinkSource,
                onStopTracking = onStopTracking,
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
            UpdatePrefsSection(
                ignoreUpdates = ignoreUpdates,
                onIgnoreUpdatesChanged = onIgnoreUpdatesChanged,
                updateCooldownDays = updateCooldownDays,
                globalCooldownDays = globalCooldownDays,
                onUpdateCooldownChanged = onUpdateCooldownChanged,
            )
            RollbackSection(
                entries = rollbackEntries,
                canSilentDowngrade = canSilentDowngrade,
                rollbackInProgress = rollbackInProgress,
                onRequestRollback = onRequestRollback,
            )
        }
        DetailSectionCard(title = stringResource(R.string.feature_apps_detail_section_installation)) {
            InstallationMeta(
                detail = detail,
                installerLabel = installerLabel,
            )
        }
        DetailSectionCard(title = stringResource(R.string.feature_apps_detail_section_security)) {
            SignatureSection(detail = detail, onCopied = onCopied)
        }
        DetailSectionCard(title = stringResource(R.string.feature_apps_detail_section_technical)) {
            PathsSection(detail = detail)
            PermissionsSection(detail = detail)
            Text(
                text = stringResource(
                    R.string.feature_apps_detail_activities_collapsed,
                    activities.size,
                ),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { activitiesExpanded = !activitiesExpanded }
                    .padding(vertical = 4.dp),
            )
            if (activitiesExpanded) {
                AppActivitiesSection(
                    activities = activities,
                    loading = activitiesLoading,
                    query = activitiesQuery,
                    onQueryChange = onActivitiesQueryChange,
                    showSearch = showActivitiesSearch,
                    onMessage = onActivityMessage,
                )
            }
        }
    }
}

@Composable
private fun DetailSectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            content()
        }
    }
}

@Composable
private fun HeroCard(detail: InstalledAppDetail, hasUpdate: Boolean) {
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
            AsyncImage(
                model = PackageIconModel(detail.packageName, detail.lastUpdateTime),
                contentDescription = null,
                modifier = Modifier.size(72.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = detail.label,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = detail.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                    ) {
                        Text(
                            text = stringResource(
                                R.string.feature_apps_detail_version,
                                detail.versionName ?: "—",
                                detail.versionCode,
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    if (detail.isSystem) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                        ) {
                            Text(
                                text = stringResource(R.string.feature_apps_detail_chip_system),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                    if (hasUpdate) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                        ) {
                            Text(
                                text = stringResource(R.string.feature_apps_detail_chip_update),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrimaryActions(
    detail: InstalledAppDetail,
    context: Context,
    availableUpdate: AppUpdateInfo?,
    installJob: InstallJobState?,
    onInstallUpdate: () -> Unit,
    onExport: () -> Unit,
) {
    val busy = installJob?.status == InstallJobStatus.DOWNLOADING ||
        installJob?.status == InstallJobStatus.INSTALLING ||
        installJob?.status == InstallJobStatus.VERIFYING
    val canUpdate = availableUpdate != null &&
        !availableUpdate.downloadUrl.isNullOrBlank() &&
        availableUpdate.signatureMatches &&
        !busy
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (detail.hasLaunchIntent) {
                FilledTonalButton(
                    onClick = {
                        context.packageManager.getLaunchIntentForPackage(detail.packageName)?.let {
                            context.startActivity(it)
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.feature_apps_action_launch))
                }
            }
            if (canUpdate) {
                Button(
                    onClick = onInstallUpdate,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.feature_apps_action_update))
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", detail.packageName, null)
                    }
                    context.startActivity(intent)
                },
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.feature_apps_action_info))
            }
            OutlinedButton(
                onClick = onExport,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.feature_apps_action_export))
            }
        }
        OutlinedButton(
            onClick = {
                val intent = Intent(Intent.ACTION_DELETE).apply {
                    data = Uri.fromParts("package", detail.packageName, null)
                }
                context.startActivity(intent)
            },
            enabled = detail.canUninstall,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.feature_apps_action_uninstall))
        }
    }
}

@Composable
private fun InstallationMeta(detail: InstalledAppDetail, installerLabel: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        MetaLine(
            label = stringResource(R.string.feature_apps_detail_meta_version),
            value = stringResource(
                R.string.feature_apps_detail_version,
                detail.versionName ?: "—",
                detail.versionCode,
            ),
        )
        MetaLine(
            label = stringResource(R.string.feature_apps_detail_meta_sdk),
            value = stringResource(R.string.feature_apps_detail_sdk, detail.minSdk, detail.targetSdk),
        )
        MetaLine(
            label = stringResource(R.string.feature_apps_detail_meta_installed),
            value = formatDate(detail.firstInstallTime),
        )
        MetaLine(
            label = stringResource(R.string.feature_apps_detail_meta_updated),
            value = formatDate(detail.lastUpdateTime),
        )
        MetaLine(
            label = stringResource(R.string.feature_apps_detail_meta_installer),
            value = installerLabel ?: stringResource(R.string.feature_apps_detail_installer_unknown),
        )
    }
}

@Composable
private fun MetaLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.35f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(0.65f),
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
        Text(stringResource(R.string.feature_apps_rollback_section), style = MaterialTheme.typography.titleSmall)
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
    onStopTracking: () -> Unit,
    onGitHubChannelChange: (GitHubReleaseChannel) -> Unit,
    hasGitHubToken: Boolean,
    showOpenInPlayStore: Boolean,
    onOpenInPlayStore: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.feature_apps_managed_source_section), style = MaterialTheme.typography.titleSmall)
        if (managedLink == null) {
            ManagedSourceUnlinked(
                showOpenInPlayStore = showOpenInPlayStore,
                onOpenInPlayStore = onOpenInPlayStore,
            )
        } else {
            ManagedSourceLinked(
                managedLink = managedLink,
                onGitHubChannelChange = onGitHubChannelChange,
                hasGitHubToken = hasGitHubToken,
            )
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
        if (managedLink != null) {
            OutlinedButton(onClick = onStopTracking, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.feature_apps_stop_tracking))
            }
        }
    }
}

@Composable
private fun ManagedSourceUnlinked(showOpenInPlayStore: Boolean, onOpenInPlayStore: () -> Unit) {
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
}

@Composable
private fun ManagedSourceLinked(
    managedLink: ManagedSourceLink,
    onGitHubChannelChange: (GitHubReleaseChannel) -> Unit,
    hasGitHubToken: Boolean,
) {
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
        ManagedGitHubChannelRow(
            selected = managedLink.githubChannel,
            onGitHubChannelChange = onGitHubChannelChange,
            hasGitHubToken = hasGitHubToken,
        )
    }
}

@Composable
private fun ManagedGitHubChannelRow(
    selected: GitHubReleaseChannel,
    onGitHubChannelChange: (GitHubReleaseChannel) -> Unit,
    hasGitHubToken: Boolean,
) {
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
                selected = selected == channel,
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
    if (selected == GitHubReleaseChannel.NIGHTLY && !hasGitHubToken) {
        Text(
            stringResource(R.string.feature_apps_github_channel_nightly_needs_pat),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun UpdatePrefsSection(
    ignoreUpdates: Boolean?,
    onIgnoreUpdatesChanged: (Boolean) -> Unit,
    updateCooldownDays: Int?,
    globalCooldownDays: Int,
    onUpdateCooldownChanged: (Int?) -> Unit,
) {
    // Switch ON means "check for updates" (ignoreUpdates == false or not yet managed).
    val checking = ignoreUpdates != true
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
        Text(
            text = stringResource(R.string.feature_apps_cooldown),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = stringResource(R.string.feature_apps_cooldown_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = updateCooldownDays == null,
                onClick = { onUpdateCooldownChanged(null) },
                label = {
                    Text(stringResource(R.string.feature_apps_cooldown_global, globalCooldownDays))
                },
            )
            FilterChip(
                selected = updateCooldownDays != null,
                onClick = { onUpdateCooldownChanged(updateCooldownDays ?: globalCooldownDays) },
                label = { Text(stringResource(R.string.feature_apps_cooldown_custom)) },
            )
        }
        if (updateCooldownDays != null) {
            Text(
                text = stringResource(R.string.feature_apps_cooldown_value, updateCooldownDays),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Slider(
                value = updateCooldownDays.toFloat(),
                onValueChange = { onUpdateCooldownChanged(it.roundToInt()) },
                valueRange = 0f..14f,
                steps = 13,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun UpdateSection(
    availableUpdate: AppUpdateInfo?,
    installJob: InstallJobState?,
    onInstallUpdate: () -> Unit,
    onRequestInstallDespiteSignature: () -> Unit,
    onOpenUnknownSources: () -> Unit,
) {
    if (availableUpdate == null && installJob == null) return
    DetailSectionCard(title = stringResource(R.string.feature_apps_update_section)) {
        if (availableUpdate != null) {
            UpdateAvailableBlock(
                update = availableUpdate,
                installJob = installJob,
                onInstallUpdate = onInstallUpdate,
                onRequestInstallDespiteSignature = onRequestInstallDespiteSignature,
            )
        }
        installJob?.let { job ->
            InstallJobStatusBlock(job = job, onOpenUnknownSources = onOpenUnknownSources)
        }
    }
}

@Composable
private fun UpdateAvailableBlock(
    update: AppUpdateInfo,
    installJob: InstallJobState?,
    onInstallUpdate: () -> Unit,
    onRequestInstallDespiteSignature: () -> Unit,
) {
    Text(
        stringResource(
            R.string.feature_apps_update_available,
            update.availableVersionName,
            update.sourceLabel,
        ),
        style = MaterialTheme.typography.bodyMedium,
    )
    val knownMismatch = !update.signatureMatches && update.confidence == MatchConfidence.WARNING
    val unknownSigner = !update.signatureMatches && !knownMismatch
    SignatureNotice(knownMismatch = knownMismatch, unknownSigner = unknownSigner)
    update.releaseNotes?.takeIf { it.isNotBlank() }?.let { notes ->
        ReleaseNotesBlock(notes = notes)
    }
    UpdateInstallActions(
        update = update,
        installJob = installJob,
        knownMismatch = knownMismatch,
        unknownSigner = unknownSigner,
        onInstallUpdate = onInstallUpdate,
        onRequestInstallDespiteSignature = onRequestInstallDespiteSignature,
    )
}

@Composable
private fun SignatureNotice(knownMismatch: Boolean, unknownSigner: Boolean) {
    when {
        knownMismatch -> Text(
            stringResource(R.string.feature_apps_update_signature_warning),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )

        unknownSigner -> Text(
            stringResource(R.string.feature_apps_update_signature_unknown),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun UpdateInstallActions(
    update: AppUpdateInfo,
    installJob: InstallJobState?,
    knownMismatch: Boolean,
    unknownSigner: Boolean,
    onInstallUpdate: () -> Unit,
    onRequestInstallDespiteSignature: () -> Unit,
) {
    val busy = installJob?.status == InstallJobStatus.DOWNLOADING ||
        installJob?.status == InstallJobStatus.INSTALLING ||
        installJob?.status == InstallJobStatus.VERIFYING
    val hasUrl = !update.downloadUrl.isNullOrBlank()
    when {
        (update.signatureMatches || unknownSigner) && hasUrl && !busy -> {
            Button(onClick = onInstallUpdate, enabled = true, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.feature_apps_action_update))
            }
        }

        knownMismatch && hasUrl && !busy -> {
            Button(onClick = onInstallUpdate, enabled = false, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.feature_apps_action_update))
            }
            OutlinedButton(onClick = onRequestInstallDespiteSignature, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.feature_apps_install_anyway))
            }
        }
    }
}

@Composable
private fun InstallJobStatusBlock(job: InstallJobState, onOpenUnknownSources: () -> Unit) {
    Text(
        stringResource(R.string.feature_apps_install_status, job.status.name, job.progressPercent),
        style = MaterialTheme.typography.bodySmall,
    )
    when (job.errorMessage) {
        "install_permission_required" -> TextButton(onClick = onOpenUnknownSources) {
            Text(stringResource(R.string.feature_apps_open_unknown_sources))
        }

        null -> Unit

        else -> Text(
            installErrorText(job.errorMessage.orEmpty()),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun installErrorText(key: String): String = when (key) {
    "signature_mismatch" -> stringResource(R.string.feature_apps_install_error_signature_mismatch)
    "install_failure_conflict" -> stringResource(R.string.feature_apps_install_error_conflict)
    else -> stringResource(R.string.feature_apps_install_error, key)
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
private fun SignatureSection(detail: InstalledAppDetail, onCopied: () -> Unit) {
    val context = LocalContext.current
    var showFull by rememberSaveable { mutableStateOf(false) }
    val first = detail.signerSha256.firstOrNull()
    if (first == null) {
        Text(
            text = "—",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    Text(
        text = if (showFull) first else first.take(SIGNATURE_PREVIEW_CHARS) + "…",
        style = MaterialTheme.typography.bodySmall,
        maxLines = if (showFull) Int.MAX_VALUE else 1,
        overflow = TextOverflow.Ellipsis,
    )
    if (showFull) {
        detail.signerSha256.drop(1).forEach { fingerprint ->
            Text(fingerprint, style = MaterialTheme.typography.bodySmall)
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        TextButton(
            onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("sha256", first))
                onCopied()
            },
        ) {
            Text(stringResource(R.string.feature_apps_detail_copy_signature))
        }
        if (detail.signerSha256.isNotEmpty()) {
            TextButton(onClick = { showFull = !showFull }) {
                Text(
                    stringResource(
                        if (showFull) {
                            R.string.feature_apps_detail_signature_hide
                        } else {
                            R.string.feature_apps_detail_signature_show_full
                        },
                    ),
                )
            }
        }
    }
}

@Composable
private fun PathsSection(detail: InstalledAppDetail) {
    Text(stringResource(R.string.feature_apps_detail_paths), style = MaterialTheme.typography.titleSmall)
    detail.apkPaths.forEach { path ->
        val size = formatApkSize(path.sizeBytes) ?: stringResource(R.string.feature_apps_size_unknown)
        val id = path.splitId ?: "base"
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("$id · $size", style = MaterialTheme.typography.bodySmall)
            Text(
                text = path.path,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PermissionsSection(detail: InstalledAppDetail) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val all = detail.requestedPermissions
    val visible = if (expanded) all else all.take(PERMISSION_COMPACT_LIMIT)
    Text(stringResource(R.string.feature_apps_detail_permissions), style = MaterialTheme.typography.titleSmall)
    visible.forEach { permission ->
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
        Text(
            text = "${permission.name}: $status$dangerous",
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
    if (all.size > PERMISSION_COMPACT_LIMIT) {
        TextButton(onClick = { expanded = !expanded }) {
            Text(
                if (expanded) {
                    stringResource(R.string.feature_apps_detail_permissions_show_less)
                } else {
                    stringResource(R.string.feature_apps_detail_permissions_show_all, all.size)
                },
            )
        }
    }
}

private fun formatDate(epochMillis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(epochMillis))

private const val PERMISSION_COMPACT_LIMIT = 8
private const val SIGNATURE_PREVIEW_CHARS = 24

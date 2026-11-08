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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.involvex.kiln.core.model.InstalledAppDetail
import dev.involvex.kiln.feature.apps.R
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
        PathsSection(detail = detail)
        PermissionsSection(detail = detail)
        if (!detail.canUninstall) {
            Text(
                text = stringResource(R.string.feature_apps_uninstall_disabled),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ActionButtons(
            detail = detail,
            context = context,
            onExport = onExport,
        )
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

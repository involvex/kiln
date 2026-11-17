package dev.involvex.kiln.feature.widgets.glance

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionSendBroadcast
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import dagger.hilt.android.EntryPointAccessors
import dev.involvex.kiln.core.common.ByteSizeFormatter
import dev.involvex.kiln.core.model.ProcessStat
import dev.involvex.kiln.core.model.ProcessStatsGate
import dev.involvex.kiln.core.model.WidgetIntents
import dev.involvex.kiln.feature.widgets.R
import dev.involvex.kiln.feature.widgets.WidgetPreferencesEntryPoint
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class KilnProcessesGlanceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetPreferencesEntryPoint::class.java,
        )
        val caps = entryPoint.privilegedExecutor().currentStatus().capabilities
        val canList = ProcessStatsGate.canList(caps)
        val canKill = ProcessStatsGate.canForceStop(caps)
        if (canList) {
            withContext(Dispatchers.IO) {
                runCatching { entryPoint.processWidgetRefresher().refreshIfStale() }
            }
        }
        val snapshot = entryPoint.widgetPreferences().current()
        val rows = entryPoint.processWidgetStore().current()
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            ?: Intent()
        val setupIntent = Intent(launch).putExtra(WidgetIntents.EXTRA_OPEN_PRIVILEGE_SETUP, true)

        provideContent {
            GlanceTheme {
                ProcessesWidgetContent(
                    title = context.getString(R.string.widget_processes_title),
                    canList = canList,
                    canKill = canKill,
                    needShizuku = context.getString(R.string.widget_processes_need_shizuku),
                    openSetupLabel = context.getString(R.string.widget_processes_open_setup),
                    refreshLabel = context.getString(R.string.widget_processes_refresh),
                    killLabel = context.getString(R.string.widget_processes_kill),
                    confirmLabel = context.getString(R.string.widget_processes_kill_confirm),
                    cancelLabel = context.getString(R.string.widget_processes_kill_cancel),
                    emptyLabel = context.getString(R.string.widget_processes_empty),
                    rows = rows,
                    killConfirmPackage = snapshot.killConfirmPackage,
                    launchIntent = launch,
                    setupIntent = setupIntent,
                    refreshIntent = Intent(context, WidgetActionReceiver::class.java)
                        .setAction(WidgetIntents.ACTION_PROCESS_REFRESH),
                    killRequestIntent = { pkg ->
                        Intent(context, WidgetActionReceiver::class.java)
                            .setAction(WidgetIntents.ACTION_KILL_REQUEST)
                            .putExtra(WidgetIntents.EXTRA_PACKAGE_NAME, pkg)
                    },
                    killConfirmIntent = { pkg ->
                        Intent(context, WidgetActionReceiver::class.java)
                            .setAction(WidgetIntents.ACTION_KILL_CONFIRM)
                            .putExtra(WidgetIntents.EXTRA_PACKAGE_NAME, pkg)
                    },
                    killCancelIntent = Intent(context, WidgetActionReceiver::class.java)
                        .setAction(WidgetIntents.ACTION_KILL_CANCEL),
                )
            }
        }
    }
}

class KilnProcessesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = KilnProcessesGlanceWidget()
}

@Composable
@Suppress("LongParameterList")
private fun ProcessesWidgetContent(
    title: String,
    canList: Boolean,
    canKill: Boolean,
    needShizuku: String,
    openSetupLabel: String,
    refreshLabel: String,
    killLabel: String,
    confirmLabel: String,
    cancelLabel: String,
    emptyLabel: String,
    rows: List<ProcessStat>,
    killConfirmPackage: String?,
    launchIntent: Intent,
    setupIntent: Intent,
    refreshIntent: Intent,
    killRequestIntent: (String) -> Intent,
    killConfirmIntent: (String) -> Intent,
    killCancelIntent: Intent,
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .padding(12.dp)
            .clickable(actionStartActivity(launchIntent)),
        verticalAlignment = Alignment.Top,
        horizontalAlignment = Alignment.Start,
    ) {
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            Text(
                text = title,
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = GlanceTheme.colors.onSurface,
                ),
                modifier = GlanceModifier.defaultWeight(),
            )
            if (canList) {
                Text(
                    text = refreshLabel,
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = GlanceTheme.colors.primary,
                    ),
                    modifier = GlanceModifier.clickable(actionSendBroadcast(refreshIntent)),
                )
            }
        }
        Spacer(modifier = GlanceModifier.height(6.dp))
        if (!canList) {
            Text(
                text = needShizuku,
                style = TextStyle(fontSize = 13.sp, color = GlanceTheme.colors.onSurfaceVariant),
            )
            Spacer(modifier = GlanceModifier.height(8.dp))
            Text(
                text = openSetupLabel,
                style = TextStyle(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = GlanceTheme.colors.primary,
                ),
                modifier = GlanceModifier.clickable(actionStartActivity(setupIntent)),
            )
            return@Column
        }
        if (rows.isEmpty()) {
            Text(
                text = emptyLabel,
                style = TextStyle(fontSize = 13.sp, color = GlanceTheme.colors.onSurfaceVariant),
            )
        } else {
            rows.forEach { row ->
                ProcessRow(
                    row = row,
                    killConfirmPackage = killConfirmPackage,
                    canKill = canKill,
                    killLabel = killLabel,
                    confirmLabel = confirmLabel,
                    cancelLabel = cancelLabel,
                    killRequestIntent = killRequestIntent,
                    killConfirmIntent = killConfirmIntent,
                    killCancelIntent = killCancelIntent,
                )
                Spacer(modifier = GlanceModifier.height(4.dp))
            }
        }
    }
}

@Composable
@Suppress("LongParameterList")
private fun ProcessRow(
    row: ProcessStat,
    killConfirmPackage: String?,
    canKill: Boolean,
    killLabel: String,
    confirmLabel: String,
    cancelLabel: String,
    killRequestIntent: (String) -> Intent,
    killConfirmIntent: (String) -> Intent,
    killCancelIntent: Intent,
) {
    val mem = ByteSizeFormatter.format(row.pssKb * 1024L)
    val cpu = row.cpuPercent?.let { String.format(Locale.US, "%.0f%%", it) } ?: "—"
    val pending = killConfirmPackage == row.packageName
    Column(modifier = GlanceModifier.fillMaxWidth()) {
        Text(
            text = "${row.label} · $mem · $cpu",
            style = TextStyle(fontSize = 11.sp, color = GlanceTheme.colors.onSurface),
            maxLines = 1,
        )
        if (!canKill) return@Column
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            if (pending) {
                Text(
                    text = confirmLabel,
                    style = TextStyle(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = GlanceTheme.colors.error,
                    ),
                    modifier = GlanceModifier
                        .padding(end = 10.dp)
                        .clickable(actionSendBroadcast(killConfirmIntent(row.packageName))),
                )
                Text(
                    text = cancelLabel,
                    style = TextStyle(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = GlanceTheme.colors.primary,
                    ),
                    modifier = GlanceModifier.clickable(actionSendBroadcast(killCancelIntent)),
                )
            } else {
                Text(
                    text = killLabel,
                    style = TextStyle(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = GlanceTheme.colors.primary,
                    ),
                    modifier = GlanceModifier.clickable(actionSendBroadcast(killRequestIntent(row.packageName))),
                )
            }
        }
    }
}

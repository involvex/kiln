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
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
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
        val processSnap = entryPoint.processWidgetStore().current()
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            ?: Intent()
        val setupIntent = Intent(launch).putExtra(WidgetIntents.EXTRA_OPEN_PRIVILEGE_SETUP, true)
        val emptyOrError = when {
            processSnap.lastErrorKey != null ->
                context.getString(R.string.widget_processes_error)

            else -> context.getString(R.string.widget_processes_empty)
        }

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
                    emptyLabel = emptyOrError,
                    rows = processSnap.rows,
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
    KilnGlassWidgetScaffold(
        modifier = GlanceModifier.clickable(actionStartActivity(launchIntent)),
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.Top,
            horizontalAlignment = Alignment.Start,
        ) {
            KilnWidgetHeaderRow(
                title = title,
                trailing = if (canList) {
                    {
                        KilnGlassChip(
                            label = refreshLabel,
                            action = actionSendBroadcast(refreshIntent),
                        )
                    }
                } else {
                    null
                },
            )
            Spacer(modifier = GlanceModifier.height(8.dp))
            if (!canList) {
                KilnWidgetBody(text = needShizuku)
                Spacer(modifier = GlanceModifier.height(10.dp))
                KilnGlassChip(
                    label = openSetupLabel,
                    action = actionStartActivity(setupIntent),
                )
                return@Column
            }
            if (rows.isEmpty()) {
                KilnWidgetBody(text = emptyLabel)
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
                    Spacer(modifier = GlanceModifier.height(6.dp))
                }
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
            text = row.label,
            style = TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = GlanceTheme.colors.onSurface,
            ),
            maxLines = 1,
        )
        Text(
            text = "$mem · $cpu",
            style = TextStyle(
                fontSize = 11.sp,
                color = GlanceTheme.colors.onSurfaceVariant,
            ),
            maxLines = 1,
        )
        if (!canKill) return@Column
        Spacer(modifier = GlanceModifier.height(4.dp))
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            if (pending) {
                KilnGlassChip(
                    label = confirmLabel,
                    action = actionSendBroadcast(killConfirmIntent(row.packageName)),
                    danger = true,
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                KilnGlassChip(
                    label = cancelLabel,
                    action = actionSendBroadcast(killCancelIntent),
                )
            } else {
                KilnGlassChip(
                    label = killLabel,
                    action = actionSendBroadcast(killRequestIntent(row.packageName)),
                )
            }
        }
    }
}

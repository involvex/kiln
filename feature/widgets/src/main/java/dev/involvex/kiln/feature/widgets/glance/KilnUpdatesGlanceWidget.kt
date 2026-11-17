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
import dev.involvex.kiln.core.model.WidgetIntents
import dev.involvex.kiln.core.model.WidgetUpdatePreviewLine
import dev.involvex.kiln.feature.widgets.R
import dev.involvex.kiln.feature.widgets.WidgetPreferencesEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class KilnUpdatesGlanceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetPreferencesEntryPoint::class.java,
        )
        withContext(Dispatchers.IO) {
            runCatching { entryPoint.widgetUpdatePublisher().refreshFromLocalIndex() }
        }
        val snapshot = entryPoint.widgetPreferences().current()
        val countLabel = if (snapshot.lastUpdateCount <= 0) {
            context.getString(R.string.widget_updates_zero)
        } else {
            context.resources.getQuantityString(
                R.plurals.widget_updates_count,
                snapshot.lastUpdateCount,
                snapshot.lastUpdateCount,
            )
        }
        val status = if (snapshot.checkQueued) {
            context.getString(R.string.widget_updates_queued)
        } else {
            countLabel
        }
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            ?: Intent()

        provideContent {
            GlanceTheme {
                UpdatesWidgetContent(
                    title = context.getString(R.string.widget_updates_title),
                    status = status,
                    preview = snapshot.updatePreview,
                    launchIntent = launch,
                    checkIntent = Intent(context, WidgetActionReceiver::class.java)
                        .setAction(WidgetIntents.ACTION_CHECK_NOW),
                    checkLabel = context.getString(R.string.widget_updates_check_now),
                )
            }
        }
    }
}

class KilnUpdatesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = KilnUpdatesGlanceWidget()
}

@Composable
private fun UpdatesWidgetContent(
    title: String,
    status: String,
    preview: List<WidgetUpdatePreviewLine>,
    launchIntent: Intent,
    checkIntent: Intent,
    checkLabel: String,
) {
    KilnGlassWidgetScaffold(
        modifier = GlanceModifier.clickable(actionStartActivity(launchIntent)),
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.Top,
            horizontalAlignment = Alignment.Start,
        ) {
            KilnWidgetHeaderRow(title = title)
            Spacer(modifier = GlanceModifier.height(6.dp))
            KilnWidgetHero(text = status)
            if (preview.isNotEmpty()) {
                Spacer(modifier = GlanceModifier.height(8.dp))
                preview.forEach { line ->
                    Text(
                        text = "${line.label} → ${line.availableVersionName}",
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            color = GlanceTheme.colors.onSurface,
                        ),
                        maxLines = 1,
                    )
                    Spacer(modifier = GlanceModifier.height(2.dp))
                }
            }
            KilnWidgetFooter {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.End,
                ) {
                    KilnGlassChip(
                        label = checkLabel,
                        action = actionSendBroadcast(checkIntent),
                    )
                }
            }
        }
    }
}

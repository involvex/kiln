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
import dev.involvex.kiln.core.model.WidgetIntents
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
                WidgetContent(
                    title = context.getString(R.string.widget_updates_title),
                    status = status,
                    trimConfirmPending = snapshot.trimConfirmPending,
                    launchIntent = launch,
                    checkIntent = Intent(context, WidgetActionReceiver::class.java)
                        .setAction(WidgetIntents.ACTION_CHECK_NOW),
                    trimRequestIntent = Intent(context, WidgetActionReceiver::class.java)
                        .setAction(WidgetIntents.ACTION_TRIM_REQUEST),
                    trimConfirmIntent = Intent(context, WidgetActionReceiver::class.java)
                        .setAction(WidgetIntents.ACTION_TRIM_CONFIRM),
                    trimCancelIntent = Intent(context, WidgetActionReceiver::class.java)
                        .setAction(WidgetIntents.ACTION_TRIM_CANCEL),
                    checkLabel = context.getString(R.string.widget_updates_check_now),
                    trimLabel = context.getString(R.string.widget_updates_trim),
                    confirmLabel = context.getString(R.string.widget_updates_trim_confirm),
                    cancelLabel = context.getString(R.string.widget_updates_trim_cancel),
                )
            }
        }
    }
}

@Composable
private fun WidgetContent(
    title: String,
    status: String,
    trimConfirmPending: Boolean,
    launchIntent: Intent,
    checkIntent: Intent,
    trimRequestIntent: Intent,
    trimConfirmIntent: Intent,
    trimCancelIntent: Intent,
    checkLabel: String,
    trimLabel: String,
    confirmLabel: String,
    cancelLabel: String,
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
        Text(
            text = title,
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        Spacer(GlanceModifier.height(4.dp))
        Text(
            text = status,
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
        Spacer(GlanceModifier.height(8.dp))
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            Text(
                text = checkLabel,
                modifier = GlanceModifier
                    .clickable(actionSendBroadcast(checkIntent))
                    .padding(4.dp),
                style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 12.sp),
            )
            if (trimConfirmPending) {
                Text(
                    text = confirmLabel,
                    modifier = GlanceModifier
                        .clickable(actionSendBroadcast(trimConfirmIntent))
                        .padding(4.dp),
                    style = TextStyle(color = GlanceTheme.colors.error, fontSize = 12.sp),
                )
                Text(
                    text = cancelLabel,
                    modifier = GlanceModifier
                        .clickable(actionSendBroadcast(trimCancelIntent))
                        .padding(4.dp),
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
                )
            } else {
                Text(
                    text = trimLabel,
                    modifier = GlanceModifier
                        .clickable(actionSendBroadcast(trimRequestIntent))
                        .padding(4.dp),
                    style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 12.sp),
                )
            }
        }
    }
}

class KilnUpdatesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = KilnUpdatesGlanceWidget()
}

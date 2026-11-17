package dev.involvex.kiln.feature.widgets.glance

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
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
import dagger.hilt.android.EntryPointAccessors
import dev.involvex.kiln.core.model.PrivilegeCapability
import dev.involvex.kiln.core.model.WidgetIntents
import dev.involvex.kiln.feature.widgets.R
import dev.involvex.kiln.feature.widgets.WidgetPreferencesEntryPoint

class KilnCacheGlanceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetPreferencesEntryPoint::class.java,
        )
        val snapshot = entryPoint.widgetPreferences().current()
        val canClear = PrivilegeCapability.CLEAR_CACHE in
            entryPoint.privilegedExecutor().currentStatus().capabilities
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            ?: Intent()
        val setupIntent = Intent(launch)
            .putExtra(WidgetIntents.EXTRA_OPEN_MANAGE, true)
            .putExtra(WidgetIntents.EXTRA_OPEN_PRIVILEGE_SETUP, true)

        provideContent {
            GlanceTheme {
                CacheWidgetContent(
                    title = context.getString(R.string.widget_cache_title),
                    explain = if (canClear) {
                        context.getString(R.string.widget_cache_explain)
                    } else {
                        context.getString(R.string.widget_cache_need_privilege)
                    },
                    confirmPending = snapshot.cacheConfirmPending,
                    canClear = canClear,
                    launchIntent = launch,
                    setupIntent = setupIntent,
                    requestIntent = Intent(context, WidgetActionReceiver::class.java)
                        .setAction(WidgetIntents.ACTION_CACHE_REQUEST),
                    confirmIntent = Intent(context, WidgetActionReceiver::class.java)
                        .setAction(WidgetIntents.ACTION_CACHE_CONFIRM),
                    cancelIntent = Intent(context, WidgetActionReceiver::class.java)
                        .setAction(WidgetIntents.ACTION_CACHE_CANCEL),
                    clearLabel = context.getString(R.string.widget_cache_clear),
                    confirmLabel = context.getString(R.string.widget_cache_confirm),
                    cancelLabel = context.getString(R.string.widget_cache_cancel),
                    openSetupLabel = context.getString(R.string.widget_cache_open_setup),
                )
            }
        }
    }
}

class KilnCacheWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = KilnCacheGlanceWidget()
}

@Composable
@Suppress("LongParameterList")
private fun CacheWidgetContent(
    title: String,
    explain: String,
    confirmPending: Boolean,
    canClear: Boolean,
    launchIntent: Intent,
    setupIntent: Intent,
    requestIntent: Intent,
    confirmIntent: Intent,
    cancelIntent: Intent,
    clearLabel: String,
    confirmLabel: String,
    cancelLabel: String,
    openSetupLabel: String,
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
            Spacer(modifier = GlanceModifier.height(8.dp))
            KilnWidgetBody(text = explain)
            KilnWidgetFooter {
                Row(modifier = GlanceModifier.fillMaxWidth()) {
                    when {
                        !canClear -> {
                            KilnGlassChip(
                                label = openSetupLabel,
                                action = actionStartActivity(setupIntent),
                            )
                        }

                        confirmPending -> {
                            KilnGlassChip(
                                label = confirmLabel,
                                action = actionSendBroadcast(confirmIntent),
                                danger = true,
                            )
                            Spacer(modifier = GlanceModifier.defaultWeight())
                            KilnGlassChip(
                                label = cancelLabel,
                                action = actionSendBroadcast(cancelIntent),
                            )
                        }

                        else -> {
                            KilnGlassChip(
                                label = clearLabel,
                                action = actionSendBroadcast(requestIntent),
                            )
                        }
                    }
                }
            }
        }
    }
}

package dev.involvex.kiln.feature.widgets.glance

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
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
import dev.involvex.kiln.feature.widgets.R

/**
 * Shared Material-style glass chrome for Kiln home widgets.
 * Glance/RemoteViews cannot blur the wallpaper; translucency + radius approximates glass.
 */
@Composable
fun KilnGlassWidgetScaffold(modifier: GlanceModifier = GlanceModifier, contentPadding: Dp = 14.dp, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .appWidgetBackground()
            .kilnGlassBackground()
            .padding(contentPadding),
    ) {
        content()
    }
}

fun GlanceModifier.kilnGlassBackground(): GlanceModifier {
    // Translucent rounded fill (day/night via color resources).
    // API 31+: also clip to the system widget corner radius.
    val withFill = background(ImageProvider(R.drawable.widget_glass_background))
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        withFill.cornerRadius(android.R.dimen.system_app_widget_background_radius)
    } else {
        withFill
    }
}

@Composable
fun KilnWidgetTitle(text: String, modifier: GlanceModifier = GlanceModifier) {
    Text(
        text = text,
        style = TextStyle(
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = GlanceTheme.colors.onSurfaceVariant,
        ),
        modifier = modifier,
    )
}

@Composable
fun KilnWidgetHero(text: String, modifier: GlanceModifier = GlanceModifier) {
    Text(
        text = text,
        style = TextStyle(
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = GlanceTheme.colors.onSurface,
        ),
        modifier = modifier,
    )
}

@Composable
fun KilnWidgetBody(text: String, modifier: GlanceModifier = GlanceModifier) {
    Text(
        text = text,
        style = TextStyle(
            fontSize = 13.sp,
            color = GlanceTheme.colors.onSurfaceVariant,
        ),
        modifier = modifier,
    )
}

@Composable
fun KilnGlassChip(
    label: String,
    action: Action,
    danger: Boolean = false,
    compact: Boolean = false,
    modifier: GlanceModifier = GlanceModifier,
) {
    val tint = if (danger) {
        GlanceTheme.colors.error
    } else {
        GlanceTheme.colors.primary
    }
    val chipBg = if (danger) {
        R.drawable.widget_glass_chip_danger
    } else {
        R.drawable.widget_glass_chip
    }
    Text(
        text = label,
        style = TextStyle(
            fontSize = if (compact) 11.sp else 12.sp,
            fontWeight = FontWeight.Medium,
            color = tint,
        ),
        modifier = modifier
            .background(ImageProvider(chipBg))
            .padding(
                horizontal = if (compact) 8.dp else 12.dp,
                vertical = if (compact) 4.dp else 7.dp,
            )
            .clickable(action),
    )
}

@Composable
fun KilnWidgetHeaderRow(title: String, trailing: (@Composable () -> Unit)? = null) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        KilnWidgetTitle(text = title, modifier = GlanceModifier.defaultWeight())
        if (trailing != null) {
            trailing()
        }
    }
}

@Composable
fun KilnWidgetFooter(content: @Composable () -> Unit) {
    Column(modifier = GlanceModifier.fillMaxWidth()) {
        Spacer(modifier = GlanceModifier.defaultWeight())
        Spacer(modifier = GlanceModifier.height(8.dp))
        content()
    }
}

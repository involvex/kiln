package dev.involvex.kiln.feature.apps.icon

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.graphics.drawable.toBitmap
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.key.Keyer
import coil3.request.Options
import coil3.size.Dimension
import kotlin.math.max
import kotlin.math.min

class PackageIconKeyer : Keyer<PackageIconModel> {
    override fun key(data: PackageIconModel, options: Options): String = "${data.packageName}:${data.lastUpdateTime}"
}

class PackageIconFetcher(private val context: Context, private val data: PackageIconModel, private val options: Options) : Fetcher {
    override suspend fun fetch(): FetchResult? = try {
        val drawable = context.packageManager.getApplicationIcon(data.packageName)
        val (width, height) = iconPixelSize(
            options = options,
            drawableWidth = drawable.intrinsicWidth,
            drawableHeight = drawable.intrinsicHeight,
        )
        val bitmap = drawable.toBitmap(width = width, height = height)
        ImageFetchResult(
            image = bitmap.asImage(),
            isSampled = true,
            // PackageManager icons live in process memory, not on a Coil disk path.
            dataSource = DataSource.MEMORY,
        )
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }

    class Factory(private val context: Context) : Fetcher.Factory<PackageIconModel> {
        override fun create(data: PackageIconModel, options: Options, imageLoader: ImageLoader): Fetcher =
            PackageIconFetcher(context.applicationContext, data, options)
    }
}

internal fun iconPixelSize(options: Options, drawableWidth: Int, drawableHeight: Int, fallbackPx: Int = DEFAULT_ICON_PX): Pair<Int, Int> {
    val reqW = dimensionPx(options.size.width, fallbackPx)
    val reqH = dimensionPx(options.size.height, fallbackPx)
    val srcW = drawableWidth.takeIf { it > 0 } ?: fallbackPx
    val srcH = drawableHeight.takeIf { it > 0 } ?: fallbackPx
    // Never upscale past a sensible list-icon ceiling; never allocate full launcher mipmaps for 40dp rows.
    val maxEdge = max(reqW, reqH).coerceIn(MIN_ICON_PX, MAX_ICON_PX)
    val scale = min(1f, maxEdge.toFloat() / max(srcW, srcH).toFloat())
    return (srcW * scale).toInt().coerceAtLeast(MIN_ICON_PX) to (srcH * scale).toInt().coerceAtLeast(MIN_ICON_PX)
}

private fun dimensionPx(dimension: Dimension, fallback: Int): Int = when (dimension) {
    is Dimension.Pixels -> dimension.px
    else -> fallback
}

private const val MIN_ICON_PX = 48
private const val DEFAULT_ICON_PX = 128
private const val MAX_ICON_PX = 192

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

class PackageIconKeyer : Keyer<PackageIconModel> {
    override fun key(data: PackageIconModel, options: Options): String = "${data.packageName}:${data.lastUpdateTime}"
}

class PackageIconFetcher(private val context: Context, private val data: PackageIconModel) : Fetcher {
    override suspend fun fetch(): FetchResult? = try {
        val drawable = context.packageManager.getApplicationIcon(data.packageName)
        val bitmap = drawable.toBitmap()
        ImageFetchResult(
            image = bitmap.asImage(),
            isSampled = false,
            dataSource = DataSource.DISK,
        )
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }

    class Factory(private val context: Context) : Fetcher.Factory<PackageIconModel> {
        override fun create(data: PackageIconModel, options: Options, imageLoader: ImageLoader): Fetcher =
            PackageIconFetcher(context.applicationContext, data)
    }
}

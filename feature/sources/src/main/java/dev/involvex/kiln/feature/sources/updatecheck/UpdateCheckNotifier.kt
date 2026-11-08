package dev.involvex.kiln.feature.sources.updatecheck

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.involvex.kiln.feature.sources.R
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class UpdateCheckNotifier @Inject constructor(@ApplicationContext private val context: Context) {
    fun notifyUpdatesFound(count: Int) {
        if (count <= 0) return
        if (!canPostNotifications()) return
        ensureChannel()
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pending = PendingIntent.getActivity(
            context,
            REQUEST_CODE,
            launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val body = context.resources.getQuantityString(
            R.plurals.update_check_notification_body,
            count,
            count,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(context.getString(R.string.update_check_notification_title))
            .setContentText(body)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (error: SecurityException) {
            Timber.w(error, "Cannot post update-check notification")
        }
    }

    private fun canPostNotifications(): Boolean {
        // API 33+: POST_NOTIFICATIONS is runtime; older platforms grant at install.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun ensureChannel() {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.update_check_notification_channel),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "kiln_update_check"
        const val NOTIFICATION_ID = 4208
        private const val REQUEST_CODE = 4208
    }
}

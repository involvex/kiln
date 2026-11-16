package dev.involvex.kiln.feature.apps.activities

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import dev.involvex.kiln.core.model.AppActivityInfo

object AppActivityActions {
    fun start(context: Context, activity: AppActivityInfo): String? = try {
        val intent = Intent().apply {
            component = ComponentName(activity.packageName, activity.className)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        null
    } catch (_: ActivityNotFoundException) {
        "activity_start_not_found"
    } catch (_: SecurityException) {
        "activity_start_security"
    }

    fun requestPinShortcut(context: Context, activity: AppActivityInfo): String {
        // requestPinShortcut is API 26+ (minSdk); still gate for clarity.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return "activity_shortcut_unsupported"
        }
        val shortcutManager = context.getSystemService(ShortcutManager::class.java)
            ?: return "activity_shortcut_unsupported"
        if (!shortcutManager.isRequestPinShortcutSupported) {
            return "activity_shortcut_unsupported"
        }
        val intent = Intent().apply {
            action = Intent.ACTION_MAIN
            component = ComponentName(activity.packageName, activity.className)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val id = "kiln.act.${activity.packageName}.${activity.className}".take(MAX_SHORTCUT_ID)
        val icon = try {
            val appInfo = context.packageManager.getApplicationInfo(activity.packageName, 0)
            Icon.createWithResource(activity.packageName, appInfo.icon)
        } catch (_: android.content.pm.PackageManager.NameNotFoundException) {
            Icon.createWithResource(context, android.R.drawable.sym_def_app_icon)
        }
        val shortcut = ShortcutInfo.Builder(context, id)
            .setShortLabel(activity.label.take(MAX_SHORTCUT_LABEL))
            .setLongLabel(activity.label.take(MAX_SHORTCUT_LABEL))
            .setIcon(icon)
            .setIntent(intent)
            .build()
        return try {
            val accepted = shortcutManager.requestPinShortcut(shortcut, null)
            if (accepted) "activity_shortcut_requested" else "activity_shortcut_rejected"
        } catch (_: IllegalArgumentException) {
            "activity_shortcut_rejected"
        } catch (_: SecurityException) {
            "activity_shortcut_rejected"
        }
    }

    private const val MAX_SHORTCUT_ID = 100
    private const val MAX_SHORTCUT_LABEL = 30
}

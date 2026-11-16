package dev.involvex.kiln.feature.apps.activities

import android.content.pm.ActivityInfo

/**
 * Pure label helpers for activity meta (unit-tested; no Android string resources).
 */
object AppActivityLabels {
    fun launchModeKey(launchMode: Int): String = when (launchMode) {
        ActivityInfo.LAUNCH_MULTIPLE -> "standard"
        ActivityInfo.LAUNCH_SINGLE_TOP -> "singleTop"
        ActivityInfo.LAUNCH_SINGLE_TASK -> "singleTask"
        ActivityInfo.LAUNCH_SINGLE_INSTANCE -> "singleInstance"
        else -> "unknown"
    }

    fun flagKeys(activityFlags: Int): List<String> = buildList {
        if (activityFlags and ActivityInfo.FLAG_EXCLUDE_FROM_RECENTS != 0) add("excludeRecent")
        if (activityFlags and ActivityInfo.FLAG_HARDWARE_ACCELERATED != 0) add("hardwareAccel")
        if (activityFlags and ActivityInfo.FLAG_NO_HISTORY != 0) add("noHistory")
        if (activityFlags and ActivityInfo.FLAG_FINISH_ON_TASK_LAUNCH != 0) add("finishOnTaskLaunch")
        if (activityFlags and ActivityInfo.FLAG_CLEAR_TASK_ON_LAUNCH != 0) add("clearTaskOnLaunch")
        if (activityFlags and ActivityInfo.FLAG_ALWAYS_RETAIN_TASK_STATE != 0) add("alwaysRetainTaskState")
        if (activityFlags and ActivityInfo.FLAG_STATE_NOT_NEEDED != 0) add("stateNotNeeded")
        if (activityFlags and ActivityInfo.FLAG_ALLOW_TASK_REPARENTING != 0) add("allowTaskReparenting")
    }

    fun matchesQuery(info: AppActivityInfoQuery, query: String): Boolean {
        val q = query.trim()
        if (q.isEmpty()) return true
        return info.label.contains(q, ignoreCase = true) ||
            info.className.contains(q, ignoreCase = true)
    }
}

/** Query projection without Android framework types in tests. */
data class AppActivityInfoQuery(val label: String, val className: String)

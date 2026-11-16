package dev.involvex.kiln.feature.apps.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.involvex.kiln.core.model.AppActivityInfo
import dev.involvex.kiln.feature.apps.R
import dev.involvex.kiln.feature.apps.activities.AppActivityActions
import dev.involvex.kiln.feature.apps.activities.AppActivityLabels

@Composable
fun AppActivitiesSection(
    activities: List<AppActivityInfo>,
    loading: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    showSearch: Boolean,
    onMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.feature_apps_activities_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.feature_apps_activities_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (showSearch) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.feature_apps_activities_search)) },
            )
        }
        when {
            loading -> CircularProgressIndicator(modifier = Modifier.size(24.dp))

            activities.isEmpty() -> Text(
                text = stringResource(R.string.feature_apps_activities_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            else -> activities.forEach { activity ->
                ActivityCard(
                    activity = activity,
                    onStart = {
                        AppActivityActions.start(context, activity)?.let(onMessage)
                    },
                    onPinShortcut = {
                        onMessage(AppActivityActions.requestPinShortcut(context, activity))
                    },
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun ActivityCard(activity: AppActivityInfo, onStart: () -> Unit, onPinShortcut: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(activity.label, style = MaterialTheme.typography.titleSmall)
            Text(
                activity.className,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(
                    R.string.feature_apps_activities_launch_mode,
                    launchModeLabel(AppActivityLabels.launchModeKey(activity.launchMode)),
                ),
                style = MaterialTheme.typography.bodySmall,
            )
            activity.taskAffinity?.let { affinity ->
                Text(
                    text = stringResource(R.string.feature_apps_activities_task_affinity, affinity),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            val flags = AppActivityLabels.flagKeys(activity.activityFlags)
            if (flags.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    flags.forEach { key ->
                        FilterChip(
                            selected = false,
                            onClick = {},
                            enabled = false,
                            label = { Text(flagLabel(key)) },
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onStart) {
                    Text(stringResource(R.string.feature_apps_activities_start))
                }
                OutlinedButton(onClick = onPinShortcut) {
                    Text(stringResource(R.string.feature_apps_activities_shortcut))
                }
            }
        }
    }
}

@Composable
private fun launchModeLabel(key: String): String = when (key) {
    "standard" -> stringResource(R.string.feature_apps_activities_mode_standard)
    "singleTop" -> stringResource(R.string.feature_apps_activities_mode_single_top)
    "singleTask" -> stringResource(R.string.feature_apps_activities_mode_single_task)
    "singleInstance" -> stringResource(R.string.feature_apps_activities_mode_single_instance)
    else -> stringResource(R.string.feature_apps_activities_mode_unknown)
}

@Composable
private fun flagLabel(key: String): String = when (key) {
    "excludeRecent" -> stringResource(R.string.feature_apps_activities_flag_exclude_recent)
    "hardwareAccel" -> stringResource(R.string.feature_apps_activities_flag_hw)
    "noHistory" -> stringResource(R.string.feature_apps_activities_flag_no_history)
    "finishOnTaskLaunch" -> stringResource(R.string.feature_apps_activities_flag_finish_on_launch)
    "clearTaskOnLaunch" -> stringResource(R.string.feature_apps_activities_flag_clear_task)
    "alwaysRetainTaskState" -> stringResource(R.string.feature_apps_activities_flag_retain_state)
    "stateNotNeeded" -> stringResource(R.string.feature_apps_activities_flag_state_not_needed)
    "allowTaskReparenting" -> stringResource(R.string.feature_apps_activities_flag_reparent)
    else -> key
}

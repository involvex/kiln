package dev.involvex.kiln.feature.apps.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.involvex.kiln.core.model.AppComponentInfo
import dev.involvex.kiln.core.model.ComponentTogglePolicy
import dev.involvex.kiln.feature.apps.R

@Composable
@Suppress("LongParameterList")
fun AppComponentsSection(
    title: String,
    hint: String,
    emptyLabel: String,
    components: List<AppComponentInfo>,
    canToggleComponents: Boolean = false,
    packageDenylisted: Boolean = false,
    toggleInProgress: Boolean = false,
    onRequestToggle: (AppComponentInfo, Boolean) -> Unit = { _, _ -> },
    onOpenPrivilegeSetup: () -> Unit = {},
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Text(
            text = hint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ComponentToggleGateCopy(
            packageDenylisted = packageDenylisted,
            canToggleComponents = canToggleComponents,
            onOpenPrivilegeSetup = onOpenPrivilegeSetup,
        )
        if (components.isEmpty()) {
            Text(
                text = emptyLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            components.forEach { component ->
                ComponentRow(
                    component = component,
                    offerToggle = ComponentTogglePolicy.canOfferToggle(
                        exported = component.exported,
                        hasDebloatCapability = canToggleComponents,
                        packageName = component.packageName,
                        denylistedPackages = if (packageDenylisted) {
                            setOf(component.packageName)
                        } else {
                            emptySet()
                        },
                    ),
                    toggleInProgress = toggleInProgress,
                    onRequestToggle = onRequestToggle,
                )
            }
        }
    }
}

@Composable
private fun ComponentToggleGateCopy(packageDenylisted: Boolean, canToggleComponents: Boolean, onOpenPrivilegeSetup: () -> Unit) {
    when {
        packageDenylisted -> {
            Text(
                text = stringResource(R.string.feature_apps_components_denylisted),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        !canToggleComponents -> {
            Text(
                text = stringResource(R.string.feature_apps_components_need_privilege),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
            TextButton(onClick = onOpenPrivilegeSetup) {
                Text(stringResource(R.string.feature_apps_components_open_setup))
            }
        }
    }
}

@Composable
private fun ComponentRow(
    component: AppComponentInfo,
    offerToggle: Boolean,
    toggleInProgress: Boolean,
    onRequestToggle: (AppComponentInfo, Boolean) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = component.label, style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = component.className,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (offerToggle) {
                    Switch(
                        checked = component.enabled,
                        onCheckedChange = { enabled ->
                            if (!toggleInProgress) onRequestToggle(component, enabled)
                        },
                        enabled = !toggleInProgress,
                    )
                }
            }
            Text(
                text = componentStatusLabel(component),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun componentStatusLabel(component: AppComponentInfo): String = when {
    component.exported && component.enabled ->
        stringResource(R.string.feature_apps_components_status_exported_enabled)

    component.exported ->
        stringResource(R.string.feature_apps_components_status_exported_disabled)

    component.enabled ->
        stringResource(R.string.feature_apps_components_status_internal_enabled)

    else ->
        stringResource(R.string.feature_apps_components_status_internal_disabled)
}

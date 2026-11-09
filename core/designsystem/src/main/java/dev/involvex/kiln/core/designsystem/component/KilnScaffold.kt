package dev.involvex.kiln.core.designsystem.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.involvex.kiln.core.designsystem.theme.KilnTheme
import dev.involvex.kiln.core.model.KilnThemeStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KilnScaffold(
    title: String,
    modifier: Modifier = Modifier,
    showTopBar: Boolean = true,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = { Text(text = title) },
                    navigationIcon = navigationIcon,
                    actions = actions,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                )
            }
        },
        bottomBar = bottomBar,
        content = content,
    )
}

@Preview(showBackground = true)
@Composable
private fun KilnScaffoldPreview() {
    KilnTheme(themeStyle = KilnThemeStyle.DARK) {
        KilnScaffold(title = "Kiln") { }
    }
}

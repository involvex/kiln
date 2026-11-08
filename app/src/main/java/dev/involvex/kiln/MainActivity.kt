package dev.involvex.kiln

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import dagger.hilt.android.AndroidEntryPoint
import dev.involvex.kiln.core.designsystem.theme.KilnTheme
import dev.involvex.kiln.ui.KilnApp

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            KilnRoot()
        }
    }
}

@Composable
private fun KilnRoot() {
    // AMOLED dark is a theme parameter only in M0 — no settings UI yet.
    KilnTheme(amoledDark = false) {
        KilnApp()
    }
}

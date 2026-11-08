package dev.involvex.kiln

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import dagger.hilt.android.AndroidEntryPoint
import dev.involvex.kiln.core.designsystem.theme.KilnTheme
import dev.involvex.kiln.feature.sources.IncomingRepoLinkStore
import dev.involvex.kiln.ui.KilnApp
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var incomingRepoLinkStore: IncomingRepoLinkStore

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        offerIncoming(intent)
        setContent {
            KilnRoot()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        offerIncoming(intent)
    }

    private fun offerIncoming(intent: Intent?) {
        val data = intent?.dataString ?: return
        if (data.isNotBlank()) {
            incomingRepoLinkStore.offer(data)
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

package dev.involvex.kiln

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.involvex.kiln.core.designsystem.theme.KilnTheme
import dev.involvex.kiln.core.model.AppearancePreferences
import dev.involvex.kiln.core.model.AppearancePrefs
import dev.involvex.kiln.core.model.KilnFontStyle
import dev.involvex.kiln.core.model.KilnLanguage
import dev.involvex.kiln.core.model.KilnThemeStyle
import dev.involvex.kiln.feature.sources.IncomingRepoLinkStore
import dev.involvex.kiln.ui.KilnApp
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var incomingRepoLinkStore: IncomingRepoLinkStore

    @Inject
    lateinit var appearancePreferences: AppearancePreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        offerIncoming(intent)
        setContent {
            val appearance by appearancePreferences.observe().collectAsStateWithLifecycle(
                initialValue = AppearancePrefs(),
            )
            ApplyLanguage(appearance.language)
            KilnRoot(themeStyle = appearance.theme, fontStyle = appearance.fontStyle)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        offerIncoming(intent)
    }

    private fun offerIncoming(intent: Intent?) {
        if (intent == null) return
        when (intent.action) {
            Intent.ACTION_SEND -> {
                if (intent.type?.startsWith("text/") == true) {
                    val text = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim().orEmpty()
                    if (text.isNotEmpty()) {
                        incomingRepoLinkStore.offer(text)
                    }
                }
            }

            else -> {
                val data = intent.dataString?.trim().orEmpty()
                if (data.isNotEmpty()) {
                    incomingRepoLinkStore.offer(data)
                }
            }
        }
    }
}

internal fun applyKilnLanguage(language: KilnLanguage) {
    val tags = when (language) {
        KilnLanguage.SYSTEM -> ""
        KilnLanguage.ENGLISH -> "en"
        KilnLanguage.GERMAN -> "de"
    }
    val desired = if (tags.isEmpty()) {
        LocaleListCompat.getEmptyLocaleList()
    } else {
        LocaleListCompat.forLanguageTags(tags)
    }
    if (AppCompatDelegate.getApplicationLocales() != desired) {
        AppCompatDelegate.setApplicationLocales(desired)
    }
}

@Composable
private fun ApplyLanguage(language: KilnLanguage) {
    LaunchedEffect(language) {
        applyKilnLanguage(language)
    }
}

@Composable
private fun KilnRoot(themeStyle: KilnThemeStyle, fontStyle: KilnFontStyle) {
    KilnTheme(themeStyle = themeStyle, fontStyle = fontStyle) {
        KilnApp()
    }
}

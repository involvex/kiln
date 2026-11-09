package dev.involvex.kiln

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import androidx.core.os.LocaleListCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.involvex.kiln.core.designsystem.theme.KilnTheme
import dev.involvex.kiln.core.model.AppearancePreferences
import dev.involvex.kiln.core.model.AppearancePrefs
import dev.involvex.kiln.core.model.KilnFontStyle
import dev.involvex.kiln.core.model.KilnLanguage
import dev.involvex.kiln.core.model.KilnLocales
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
    val current = AppCompatDelegate.getApplicationLocales().toLanguageTags()
    if (KilnLocales.isSatisfied(language, current)) return

    val desired = if (language == KilnLanguage.SYSTEM) {
        // Empty list = follow system. forLanguageTags("") does not always
        // round-trip on HyperOS the same way getEmptyLocaleList() does.
        LocaleListCompat.getEmptyLocaleList()
    } else {
        LocaleListCompat.forLanguageTags(KilnLocales.desiredLanguageTags(language))
    }
    AppCompatDelegate.setApplicationLocales(desired)
}

@Composable
private fun ApplyLanguage(language: KilnLanguage) {
    LaunchedEffect(language) {
        applyKilnLanguage(language)
    }
}

@Composable
private fun KilnRoot(themeStyle: KilnThemeStyle, fontStyle: KilnFontStyle) {
    val view = LocalView.current
    // dynamicColor=false: HyperOS Monet / wallpaper shifts were fighting the
    // fixed Theme.Kiln windowBackground and flashing the whole surface.
    KilnTheme(themeStyle = themeStyle, fontStyle = fontStyle, dynamicColor = false) {
        val lightBars = themeStyle == KilnThemeStyle.LIGHT
        if (!view.isInEditMode) {
            SideEffect {
                val controller = WindowCompat.getInsetsController(
                    (view.context as android.app.Activity).window,
                    view,
                )
                controller.isAppearanceLightStatusBars = lightBars
                controller.isAppearanceLightNavigationBars = lightBars
            }
        }
        KilnApp()
    }
}

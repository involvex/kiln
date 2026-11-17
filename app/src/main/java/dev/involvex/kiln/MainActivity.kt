package dev.involvex.kiln

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.os.LocaleListCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.involvex.kiln.core.designsystem.theme.KilnTheme
import dev.involvex.kiln.core.designsystem.theme.usesLightSystemBars
import dev.involvex.kiln.core.model.AppearancePreferences
import dev.involvex.kiln.core.model.AppearancePrefs
import dev.involvex.kiln.core.model.KilnFontStyle
import dev.involvex.kiln.core.model.KilnLanguage
import dev.involvex.kiln.core.model.KilnLocales
import dev.involvex.kiln.core.model.KilnThemeStyle
import dev.involvex.kiln.core.model.WidgetIntents
import dev.involvex.kiln.feature.apps.apktools.IncomingApkInstallStore
import dev.involvex.kiln.feature.sources.IncomingRepoLinkStore
import dev.involvex.kiln.feature.widgets.ManageDeepLinkStore
import dev.involvex.kiln.feature.widgets.PrivilegeSetupDeepLinkStore
import dev.involvex.kiln.ui.KilnApp
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var incomingRepoLinkStore: IncomingRepoLinkStore

    @Inject
    lateinit var incomingApkInstallStore: IncomingApkInstallStore

    @Inject
    lateinit var appearancePreferences: AppearancePreferences

    @Inject
    lateinit var manageDeepLinkStore: ManageDeepLinkStore

    @Inject
    lateinit var privilegeSetupDeepLinkStore: PrivilegeSetupDeepLinkStore

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        offerIncoming(intent)
        setContent {
            // Theme/font only — language is applied in KilnApplication.
            // Do NOT call setApplicationLocales from Compose: collectAsStateWithLifecycle
            // starts with AppearancePrefs() (SYSTEM) before DataStore emits, which fought
            // the real preference (en ↔ de-DE) and relaunched MainActivity ~6×/s on HyperOS.
            val appearance by appearancePreferences.observe().collectAsStateWithLifecycle(
                initialValue = AppearancePrefs(),
            )
            KilnRoot(
                themeStyle = appearance.theme,
                fontStyle = appearance.fontStyle,
                dynamicColor = appearance.dynamicColor,
                manageDeepLinkStore = manageDeepLinkStore,
                privilegeSetupDeepLinkStore = privilegeSetupDeepLinkStore,
                incomingApkInstallStore = incomingApkInstallStore,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        offerIncoming(intent)
    }

    private fun offerIncoming(intent: Intent?) {
        if (intent == null) return
        offerWidgetDeepLinks(intent)
        when (intent.action) {
            Intent.ACTION_SEND -> offerSendIntent(intent)

            Intent.ACTION_VIEW -> offerViewIntent(intent)

            else -> {
                val data = intent.dataString?.trim().orEmpty()
                if (data.isNotEmpty()) {
                    incomingRepoLinkStore.offer(data)
                }
            }
        }
    }

    private fun offerWidgetDeepLinks(intent: Intent) {
        if (intent.getBooleanExtra(WidgetIntents.EXTRA_OPEN_MANAGE, false)) {
            manageDeepLinkStore.offer()
        }
        if (intent.getBooleanExtra(WidgetIntents.EXTRA_OPEN_PRIVILEGE_SETUP, false)) {
            privilegeSetupDeepLinkStore.offer()
        }
    }

    private fun offerSendIntent(intent: Intent) {
        if (intent.type?.startsWith("text/") == true) {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim().orEmpty()
            if (text.isNotEmpty()) {
                incomingRepoLinkStore.offer(text)
            }
            return
        }
        val stream = intent.parcelableUriExtra(Intent.EXTRA_STREAM)
        if (stream != null && looksLikeApkOrBundle(stream, intent.type)) {
            offerApkInstall(stream)
        }
    }

    private fun offerViewIntent(intent: Intent) {
        val data = intent.data
        if (data != null && looksLikeApkOrBundle(data, intent.type)) {
            offerApkInstall(data)
            return
        }
        val dataString = intent.dataString?.trim().orEmpty()
        if (dataString.isNotEmpty()) {
            incomingRepoLinkStore.offer(dataString)
        }
    }

    private fun offerApkInstall(uri: Uri) {
        incomingApkInstallStore.offer(uri)
        manageDeepLinkStore.offer()
    }

    private fun looksLikeApkOrBundle(uri: Uri, mimeType: String?): Boolean {
        val mime = mimeType?.lowercase(Locale.US).orEmpty()
        if (mime == "application/vnd.android.package-archive" ||
            mime == "application/zip" ||
            mime == "application/octet-stream"
        ) {
            return true
        }
        val name = (uri.lastPathSegment ?: uri.toString()).lowercase(Locale.US)
        return name.endsWith(".apk") || name.endsWith(".apks") ||
            name.endsWith(".xapk") || name.endsWith(".apkm")
    }
}

@Suppress("DEPRECATION")
private fun Intent.parcelableUriExtra(key: String): Uri? =
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(key, Uri::class.java)
    } else {
        getParcelableExtra(key) as? Uri
    }

/** Last language we asked AppCompat to apply in this process (stops SYSTEM OEM tag thrash). */
@Volatile
private var lastAppliedLanguage: KilnLanguage? = null

internal fun applyKilnLanguage(language: KilnLanguage) {
    val current = AppCompatDelegate.getApplicationLocales().toLanguageTags()
    if (KilnLocales.isSatisfied(language, current)) {
        lastAppliedLanguage = language
        return
    }
    // HyperOS may report resolved system tags (e.g. de-DE) even after
    // setApplicationLocales(empty). Re-applying SYSTEM would relaunch forever.
    if (language == KilnLanguage.SYSTEM && lastAppliedLanguage == KilnLanguage.SYSTEM) {
        return
    }

    val desired = if (language == KilnLanguage.SYSTEM) {
        LocaleListCompat.getEmptyLocaleList()
    } else {
        LocaleListCompat.forLanguageTags(KilnLocales.desiredLanguageTags(language))
    }
    AppCompatDelegate.setApplicationLocales(desired)
    lastAppliedLanguage = language
}

@Composable
private fun KilnRoot(
    themeStyle: KilnThemeStyle,
    fontStyle: KilnFontStyle,
    dynamicColor: Boolean,
    manageDeepLinkStore: ManageDeepLinkStore,
    privilegeSetupDeepLinkStore: PrivilegeSetupDeepLinkStore,
    incomingApkInstallStore: IncomingApkInstallStore,
) {
    val view = LocalView.current
    val systemDark = isSystemInDarkTheme()
    KilnTheme(themeStyle = themeStyle, fontStyle = fontStyle, dynamicColor = dynamicColor) {
        val lightBars = themeStyle.usesLightSystemBars(systemDark)
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
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            KilnApp(
                manageDeepLinkStore = manageDeepLinkStore,
                privilegeSetupDeepLinkStore = privilegeSetupDeepLinkStore,
                incomingApkInstallStore = incomingApkInstallStore,
            )
        }
    }
}

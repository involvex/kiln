package dev.involvex.kiln.feature.apps.denylist

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.provider.Telephony
import android.telecom.TelecomManager
import android.view.inputmethod.InputMethodManager
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.involvex.kiln.core.model.ProtectedPackages
import javax.inject.Inject
import javax.inject.Singleton

/** Same hard + dynamic roles as Debloat; kept in :feature:apps (no feature→feature deps). */
@Singleton
class AppProtectedPackageResolver @Inject constructor(@ApplicationContext private val context: Context) {
    fun resolve(): Map<String, String> {
        val hits = LinkedHashMap<String, String>()
        hits[context.packageName] = ProtectedPackages.REASON_SELF
        for (pkg in ProtectedPackages.HARD_PACKAGES) {
            hits.putIfAbsent(pkg, ProtectedPackages.REASON_HARD)
        }
        defaultLauncher()?.let { hits.putIfAbsent(it, ProtectedPackages.REASON_LAUNCHER) }
        defaultIme()?.let { hits.putIfAbsent(it, ProtectedPackages.REASON_IME) }
        defaultDialer()?.let { hits.putIfAbsent(it, ProtectedPackages.REASON_DIALER) }
        defaultSms()?.let { hits.putIfAbsent(it, ProtectedPackages.REASON_SMS) }
        return hits
    }

    private fun defaultLauncher(): String? {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolve = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolve?.activityInfo?.packageName
    }

    private fun defaultIme(): String? {
        val id = Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
            ?: return null
        val pkg = id.substringBefore('/')
        if (pkg.isNotBlank()) return pkg
        val imm = context.getSystemService(InputMethodManager::class.java) ?: return null
        return imm.enabledInputMethodList.firstOrNull()?.packageName
    }

    private fun defaultDialer(): String? {
        val telecom = context.getSystemService(TelecomManager::class.java)
        return telecom?.defaultDialerPackage
    }

    private fun defaultSms(): String? = Telephony.Sms.getDefaultSmsPackage(context)
}

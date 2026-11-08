package dev.involvex.kiln.feature.debloat.denylist

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.provider.Telephony
import android.telecom.TelecomManager
import android.view.inputmethod.InputMethodManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DebloatDenylistResolver @Inject constructor(@ApplicationContext private val context: Context) {
    fun resolve(): Map<String, String> {
        val hits = LinkedHashMap<String, String>()
        hits[context.packageName] = DebloatDenylist.REASON_SELF
        for (pkg in DebloatDenylist.HARD_PACKAGES) {
            hits.putIfAbsent(pkg, DebloatDenylist.REASON_HARD)
        }
        defaultLauncher()?.let { hits.putIfAbsent(it, DebloatDenylist.REASON_LAUNCHER) }
        defaultIme()?.let { hits.putIfAbsent(it, DebloatDenylist.REASON_IME) }
        defaultDialer()?.let { hits.putIfAbsent(it, DebloatDenylist.REASON_DIALER) }
        defaultSms()?.let { hits.putIfAbsent(it, DebloatDenylist.REASON_SMS) }
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
        // Prefer TelecomManager (API 23+); RoleManager.getRoleHolders needs special holdership APIs.
        val telecom = context.getSystemService(TelecomManager::class.java)
        return telecom?.defaultDialerPackage
    }

    private fun defaultSms(): String? = Telephony.Sms.getDefaultSmsPackage(context)
}

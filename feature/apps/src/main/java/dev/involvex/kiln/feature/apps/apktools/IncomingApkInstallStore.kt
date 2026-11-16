package dev.involvex.kiln.feature.apps.apktools

import android.net.Uri
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Holds a shared / VIEW APK or split-bundle URI until APK tools Install consumes it. */
@Singleton
class IncomingApkInstallStore @Inject constructor() {
    private val _pending = MutableStateFlow<Uri?>(null)
    val pending: StateFlow<Uri?> = _pending.asStateFlow()

    fun offer(uri: Uri) {
        _pending.value = uri
    }

    fun consume(): Uri? {
        val value = _pending.value
        _pending.value = null
        return value
    }
}

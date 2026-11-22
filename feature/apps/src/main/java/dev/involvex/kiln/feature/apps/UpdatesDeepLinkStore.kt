package dev.involvex.kiln.feature.apps

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * One-shot signal so MainActivity/KilnApp can open Apps and
 * [AppsViewModel] can select the Updates filter when an update
 * notification (or Updates widget) is tapped.
 */
@Singleton
class UpdatesDeepLinkStore @Inject constructor() {
    private val _pending = MutableStateFlow(false)
    val pending: StateFlow<Boolean> = _pending.asStateFlow()

    fun offer() {
        _pending.value = true
    }

    fun consume(): Boolean {
        val value = _pending.value
        _pending.value = false
        return value
    }
}

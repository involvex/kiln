package dev.involvex.kiln.feature.widgets

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * One-shot signal so MainActivity/KilnApp can open the Manage tab
 * when Trim caches cannot run without privilege.
 */
@Singleton
class ManageDeepLinkStore @Inject constructor() {
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

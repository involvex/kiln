package dev.involvex.kiln.feature.sources

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Holds a VIEW intent URI until Sources consumes it (fdroidrepo / https?fingerprint). */
@Singleton
class IncomingRepoLinkStore @Inject constructor() {
    private val _pending = MutableStateFlow<String?>(null)
    val pending: StateFlow<String?> = _pending.asStateFlow()

    fun offer(uri: String) {
        _pending.value = uri
    }

    fun consume(): String? {
        val value = _pending.value
        _pending.value = null
        return value
    }
}

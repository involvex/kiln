package dev.involvex.kiln.feature.termux.run

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

@Singleton
class TermuxResultBus @Inject constructor() {
    private val _results = MutableSharedFlow<TermuxCommandResult>(extraBufferCapacity = 8)
    val results: SharedFlow<TermuxCommandResult> = _results.asSharedFlow()

    fun publish(result: TermuxCommandResult) {
        _results.tryEmit(result)
    }
}

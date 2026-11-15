package dev.involvex.kiln.feature.termux

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.involvex.kiln.feature.termux.run.TermuxResultBus

@EntryPoint
@InstallIn(SingletonComponent::class)
interface TermuxResultReceiverEntryPoint {
    fun termuxResultBus(): TermuxResultBus
}

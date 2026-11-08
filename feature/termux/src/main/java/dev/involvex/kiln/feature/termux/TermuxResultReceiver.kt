package dev.involvex.kiln.feature.termux

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import dev.involvex.kiln.feature.termux.run.TermuxCommandResult
import dev.involvex.kiln.feature.termux.run.TermuxResultBus
import javax.inject.Inject
import timber.log.Timber

@AndroidEntryPoint
class TermuxResultReceiver : BroadcastReceiver() {
    @Inject
    lateinit var resultBus: TermuxResultBus

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val result = TermuxCommandResult.fromIntent(intent)
        if (result == null) {
            Timber.w("Termux result intent missing execution id")
            return
        }
        Timber.d(
            "Termux result id=%s exit=%s err=%s",
            result.executionId,
            result.exitCode,
            result.err,
        )
        resultBus.publish(result)
    }
}

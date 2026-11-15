package dev.involvex.kiln.feature.termux

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.EntryPointAccessors
import dev.involvex.kiln.feature.termux.run.TermuxCommandResult
import timber.log.Timber

/**
 * Termux result callback. Uses [EntryPointAccessors] instead of [@AndroidEntryPoint]
 * so AGP does not need to ASM-rewrite this library BroadcastReceiver (CI has failed
 * that transform when Hilt's generated Java superclass was missing / javac NO-SOURCE).
 */
class TermuxResultReceiver : BroadcastReceiver() {
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
        val bus = EntryPointAccessors.fromApplication(
            context.applicationContext,
            TermuxResultReceiverEntryPoint::class.java,
        ).termuxResultBus()
        bus.publish(result)
    }
}

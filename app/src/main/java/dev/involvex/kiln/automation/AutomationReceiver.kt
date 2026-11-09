package dev.involvex.kiln.automation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import dev.involvex.kiln.core.common.IoDispatcher
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Exported automation entry point. Requires Settings → Automation enabled and
 * [dev.involvex.kiln.core.model.AutomationApi.PERMISSION] (signature).
 */
@AndroidEntryPoint
class AutomationReceiver : BroadcastReceiver() {
    @Inject
    lateinit var handler: AutomationCommandHandler

    @Inject
    @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val pending = goAsync()
        val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
        scope.launch {
            try {
                val outcome = handler.handle(intent)
                Timber.d("Automation %s → ok=%s msg=%s", intent.action, outcome.ok, outcome.message)
            } catch (@Suppress("TooGenericExceptionCaught") error: RuntimeException) {
                Timber.e(error, "Automation handler crashed for %s", intent.action)
            } finally {
                pending.finish()
            }
        }
    }
}

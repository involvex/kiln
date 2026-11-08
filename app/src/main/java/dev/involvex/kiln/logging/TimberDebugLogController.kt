package dev.involvex.kiln.logging

import android.content.Context
import android.content.pm.ApplicationInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.involvex.kiln.core.model.DebugLogController
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class TimberDebugLogController @Inject constructor(@ApplicationContext private val context: Context) : DebugLogController {
    @Volatile
    private var plantedTree: Timber.Tree? = null

    private val isDebuggable: Boolean
        get() = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    override fun apply(enabled: Boolean) {
        val wantDebugTree = enabled || isDebuggable
        synchronized(this) {
            if (wantDebugTree) {
                if (plantedTree == null) {
                    val tree = Timber.DebugTree()
                    Timber.plant(tree)
                    plantedTree = tree
                    Timber.d("Debug log tree planted (user=%s debuggable=%s)", enabled, isDebuggable)
                }
            } else {
                plantedTree?.let { Timber.uproot(it) }
                plantedTree = null
            }
        }
    }
}

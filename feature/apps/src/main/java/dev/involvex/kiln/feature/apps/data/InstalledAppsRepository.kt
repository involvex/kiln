package dev.involvex.kiln.feature.apps.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.involvex.kiln.core.common.IoDispatcher
import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.core.model.InstalledAppDetail
import dev.involvex.kiln.core.model.InstalledAppSummary
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import timber.log.Timber

interface InstalledAppsRepository {
    val summaries: StateFlow<List<InstalledAppSummary>>

    val isLoading: StateFlow<Boolean>

    val lastError: StateFlow<String?>

    fun refresh()

    suspend fun detail(packageName: String): Result<InstalledAppDetail>

    fun isInstalled(packageName: String): Boolean
}

@Singleton
class DefaultInstalledAppsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val packageInfoSource: PackageInfoSource,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : InstalledAppsRepository {
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val manualRefresh = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private val _isLoading = MutableStateFlow(true)
    override val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    override val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val packageEvents: Flow<Unit> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                trySend(Unit)
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        // API 33+: RECEIVER_NOT_EXPORTED required for dynamically registered receivers.
        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        trySend(Unit)
        awaitClose {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

    private val foregroundEvents: Flow<Unit> = callbackFlow {
        val observer = object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                trySend(Unit)
            }
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(observer)
        awaitClose {
            ProcessLifecycleOwner.get().lifecycle.removeObserver(observer)
        }
    }

    private val refreshTicks = combine(
        packageEvents.onStart { emit(Unit) },
        foregroundEvents.onStart { emit(Unit) },
        manualRefresh.onStart { emit(Unit) },
    ) { _, _, _ -> Unit }

    override val summaries: StateFlow<List<InstalledAppSummary>> = refreshTicks
        .map {
            _isLoading.value = true
            try {
                val list = packageInfoSource.getInstalledSummaries()
                _lastError.value = null
                list
            } catch (@Suppress("TooGenericExceptionCaught") error: RuntimeException) {
                Timber.w(error, "Failed to load installed packages")
                _lastError.value = error.message ?: "load_failed"
                emptyList()
            } finally {
                _isLoading.value = false
            }
        }
        .flowOn(ioDispatcher)
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    override fun refresh() {
        manualRefresh.tryEmit(Unit)
    }

    override suspend fun detail(packageName: String): Result<InstalledAppDetail> = withContext(ioDispatcher) {
        try {
            val detail = packageInfoSource.getDetail(packageName)
            if (detail == null) {
                Result.failure("Package not found: $packageName")
            } else {
                Result.success(detail)
            }
        } catch (@Suppress("TooGenericExceptionCaught") error: RuntimeException) {
            Result.failure(error.message ?: "detail_failed", error)
        }
    }

    override fun isInstalled(packageName: String): Boolean = packageInfoSource.isPackageInstalled(packageName)
}

package dev.involvex.kiln.feature.maintenance

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.involvex.kiln.core.common.IoDispatcher
import dev.involvex.kiln.core.model.MaintenanceDefaults
import dev.involvex.kiln.core.model.MaintenanceRunRecord
import dev.involvex.kiln.core.model.MaintenanceRunStore
import dev.involvex.kiln.core.model.MaintenanceTaskResult
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber

@Singleton
class FileMaintenanceRunStore @Inject constructor(
    @ApplicationContext context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : MaintenanceRunStore {
    private val file = File(File(context.filesDir, "maintenance").also { it.mkdirs() }, "runs.json")
    private val mutex = Mutex()
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
    }
    private val state = MutableStateFlow(loadLocked())

    override fun observe(): Flow<List<MaintenanceRunRecord>> = state.asStateFlow()

    override suspend fun latest(): MaintenanceRunRecord? = state.value.firstOrNull()

    override suspend fun append(record: MaintenanceRunRecord) = mutex.withLock {
        withContext(ioDispatcher) {
            val next = (listOf(record) + state.value).take(MaintenanceDefaults.MAX_RUN_RECORDS)
            try {
                file.writeText(json.encodeToString(next.map { it.toDto() }))
            } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
                Timber.w(error, "Failed to persist maintenance protocol")
            }
            state.value = next
        }
    }

    private fun loadLocked(): List<MaintenanceRunRecord> {
        if (!file.isFile) return emptyList()
        return try {
            json.decodeFromString<List<MaintenanceRunDto>>(file.readText()).map { it.toDomain() }
        } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
            Timber.w(error, "Failed to read maintenance protocol")
            emptyList()
        }
    }

    @Serializable
    private data class MaintenanceRunDto(
        val startedAtEpochMs: Long,
        val finishedAtEpochMs: Long,
        val ok: Boolean,
        val tasks: List<MaintenanceTaskDto> = emptyList(),
    )

    @Serializable
    private data class MaintenanceTaskDto(val taskId: String, val status: String, val detail: String = "", val bytesFreed: Long = 0L)

    private fun MaintenanceRunRecord.toDto() = MaintenanceRunDto(
        startedAtEpochMs = startedAtEpochMs,
        finishedAtEpochMs = finishedAtEpochMs,
        ok = ok,
        tasks = tasks.map {
            MaintenanceTaskDto(it.taskId, it.status, it.detail, it.bytesFreed)
        },
    )

    private fun MaintenanceRunDto.toDomain() = MaintenanceRunRecord(
        startedAtEpochMs = startedAtEpochMs,
        finishedAtEpochMs = finishedAtEpochMs,
        ok = ok,
        tasks = tasks.map {
            MaintenanceTaskResult(it.taskId, it.status, it.detail, it.bytesFreed)
        },
    )
}

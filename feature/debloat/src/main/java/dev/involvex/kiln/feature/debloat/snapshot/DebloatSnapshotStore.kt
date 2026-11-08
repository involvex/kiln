package dev.involvex.kiln.feature.debloat.snapshot

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.involvex.kiln.core.common.IoDispatcher
import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.core.model.DebloatPackageState
import dev.involvex.kiln.core.model.DebloatSnapshot
import dev.involvex.kiln.core.model.DebloatSnapshotPackage
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Singleton
class DebloatSnapshotStore @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private fun file(): File = File(context.filesDir, "debloat/snapshot.json").also { it.parentFile?.mkdirs() }

    suspend fun load(): DebloatSnapshot? = withContext(ioDispatcher) {
        val f = file()
        if (!f.exists()) return@withContext null
        runCatching {
            val raw = json.decodeFromString(SnapshotFile.serializer(), f.readText())
            DebloatSnapshot(
                createdAtEpochMs = raw.createdAtEpochMs,
                packages = raw.packages.map {
                    DebloatSnapshotPackage(
                        packageName = it.packageName,
                        state = DebloatPackageState.valueOf(it.state),
                    )
                },
            )
        }.getOrNull()
    }

    suspend fun save(snapshot: DebloatSnapshot): Result<Unit> = withContext(ioDispatcher) {
        try {
            val raw = SnapshotFile(
                createdAtEpochMs = snapshot.createdAtEpochMs,
                packages = snapshot.packages.map {
                    SnapshotPackageFile(it.packageName, it.state.name)
                },
            )
            file().writeText(json.encodeToString(SnapshotFile.serializer(), raw))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure("snapshot_write_failed", e)
        }
    }

    @Serializable
    private data class SnapshotFile(val createdAtEpochMs: Long, val packages: List<SnapshotPackageFile>)

    @Serializable
    private data class SnapshotPackageFile(val packageName: String, val state: String)
}

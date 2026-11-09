package dev.involvex.kiln.core.database

import dev.involvex.kiln.core.common.IoDispatcher
import dev.involvex.kiln.core.model.ActivityLogDefaults
import dev.involvex.kiln.core.model.ActivityLogEvent
import dev.involvex.kiln.core.model.ActivityLogFilter
import dev.involvex.kiln.core.model.ActivityLogQuery
import dev.involvex.kiln.core.model.ActivityLogRecorder
import dev.involvex.kiln.core.model.ActivityLogRepository
import dev.involvex.kiln.core.model.ActivityLogType
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber

@Singleton
class RoomActivityLogStore @Inject constructor(
    private val dao: ActivityLogDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ActivityLogRecorder,
    ActivityLogRepository {
    override suspend fun record(event: ActivityLogEvent) = withContext(ioDispatcher) {
        try {
            dao.insert(event.toEntity())
            val overflow = dao.count() - ActivityLogDefaults.MAX_ENTRIES
            if (overflow > 0) {
                dao.deleteOldest(overflow)
            }
        } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
            Timber.w(error, "Failed to record activity log event")
        }
    }

    override fun observe(query: ActivityLogQuery): Flow<List<ActivityLogEvent>> {
        val typeName = query.type?.name.orEmpty()
        val limit = query.limit.coerceIn(1, ActivityLogDefaults.MAX_ENTRIES)
        val source = if (typeName.isEmpty()) {
            dao.observeRecent(limit)
        } else {
            dao.observeFiltered(typeName, limit)
        }
        return source.map { rows ->
            val mapped = rows.map { it.toDomain() }
            ActivityLogFilter.filter(mapped, query.search, query.type)
        }
    }

    override suspend fun clearAll() = withContext(ioDispatcher) {
        try {
            dao.deleteAll()
        } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
            Timber.w(error, "Failed to clear activity log")
        }
    }

    private fun ActivityLogEvent.toEntity() = ActivityLogEntity(
        id = id,
        epochMs = epochMs,
        type = type.name,
        action = action,
        packageName = packageName,
        versionName = versionName,
        versionCode = versionCode,
        success = success,
        detailKey = detailKey,
        refId = refId,
    )

    private fun ActivityLogEntity.toDomain() = ActivityLogEvent(
        id = id,
        epochMs = epochMs,
        type = runCatching { ActivityLogType.valueOf(type) }.getOrDefault(ActivityLogType.AUTOMATION),
        action = action,
        packageName = packageName,
        versionName = versionName,
        versionCode = versionCode,
        success = success,
        detailKey = detailKey,
        refId = refId,
    )
}

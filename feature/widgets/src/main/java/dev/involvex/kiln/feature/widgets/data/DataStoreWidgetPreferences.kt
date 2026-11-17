package dev.involvex.kiln.feature.widgets.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.involvex.kiln.core.model.WidgetPreferences
import dev.involvex.kiln.core.model.WidgetSnapshot
import dev.involvex.kiln.core.model.WidgetUpdatePreview
import dev.involvex.kiln.core.model.WidgetUpdatePreviewLine
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.widgetPrefsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "kiln_widget_prefs",
)

@Singleton
class DataStoreWidgetPreferences @Inject constructor(@ApplicationContext context: Context) : WidgetPreferences {
    private val store = context.widgetPrefsDataStore

    override fun observe(): Flow<WidgetSnapshot> = store.data.map { it.toSnapshot() }

    override suspend fun current(): WidgetSnapshot = store.data.first().toSnapshot()

    override suspend fun setUpdateCount(count: Int, preview: List<WidgetUpdatePreviewLine>, epochMs: Long) {
        store.edit {
            it[KEY_COUNT] = count.coerceAtLeast(0)
            it[KEY_EPOCH] = epochMs
            it[KEY_CHECK_QUEUED] = false
            it[KEY_PREVIEW] = encodePreview(preview.take(WidgetUpdatePreview.MAX_LINES))
        }
    }

    override suspend fun setCheckQueued(queued: Boolean) {
        store.edit { it[KEY_CHECK_QUEUED] = queued }
    }

    override suspend fun setCacheConfirmPending(pending: Boolean) {
        store.edit { it[KEY_CACHE_CONFIRM] = pending }
    }

    override suspend fun setKillConfirmPackage(packageName: String?) {
        store.edit {
            if (packageName.isNullOrBlank()) {
                it.remove(KEY_KILL_PACKAGE)
            } else {
                it[KEY_KILL_PACKAGE] = packageName
            }
        }
    }

    override suspend fun setLastProcessRefreshEpochMs(epochMs: Long) {
        store.edit { it[KEY_PROCESS_REFRESH_EPOCH] = epochMs }
    }

    private fun Preferences.toSnapshot() = WidgetSnapshot(
        lastUpdateCount = this[KEY_COUNT] ?: 0,
        lastCheckedEpochMs = this[KEY_EPOCH] ?: 0L,
        checkQueued = this[KEY_CHECK_QUEUED] ?: false,
        updatePreview = decodePreview(this[KEY_PREVIEW].orEmpty()),
        cacheConfirmPending = this[KEY_CACHE_CONFIRM] ?: false,
        killConfirmPackage = this[KEY_KILL_PACKAGE],
        lastProcessRefreshEpochMs = this[KEY_PROCESS_REFRESH_EPOCH] ?: 0L,
    )

    companion object {
        internal fun encodePreview(lines: List<WidgetUpdatePreviewLine>): String = lines.joinToString("\n") { line ->
            listOf(line.packageName, escape(line.label), escape(line.availableVersionName))
                .joinToString("\t")
        }

        internal fun decodePreview(raw: String): List<WidgetUpdatePreviewLine> {
            if (raw.isBlank()) return emptyList()
            return raw.lineSequence().mapNotNull { line ->
                val parts = line.split('\t')
                if (parts.size < 3) return@mapNotNull null
                WidgetUpdatePreviewLine(
                    packageName = parts[0],
                    label = unescape(parts[1]),
                    availableVersionName = unescape(parts[2]),
                )
            }.toList()
        }

        private fun escape(value: String): String = value.replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n")

        private fun unescape(value: String): String = buildString {
            var i = 0
            while (i < value.length) {
                val c = value[i]
                if (c == '\\' && i + 1 < value.length) {
                    when (value[i + 1]) {
                        't' -> {
                            append('\t')
                            i += 2
                        }

                        'n' -> {
                            append('\n')
                            i += 2
                        }

                        '\\' -> {
                            append('\\')
                            i += 2
                        }

                        else -> {
                            append(c)
                            i++
                        }
                    }
                } else {
                    append(c)
                    i++
                }
            }
        }

        private val KEY_COUNT = intPreferencesKey("last_update_count")
        private val KEY_EPOCH = longPreferencesKey("last_checked_epoch_ms")
        private val KEY_CHECK_QUEUED = booleanPreferencesKey("check_queued")
        private val KEY_PREVIEW = stringPreferencesKey("update_preview_lines")
        private val KEY_CACHE_CONFIRM = booleanPreferencesKey("cache_confirm_pending")
        private val KEY_KILL_PACKAGE = stringPreferencesKey("kill_confirm_package")
        private val KEY_PROCESS_REFRESH_EPOCH = longPreferencesKey("process_refresh_epoch_ms")
    }
}

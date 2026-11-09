package dev.involvex.kiln.feature.widgets

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.involvex.kiln.core.model.WidgetPreferences
import dev.involvex.kiln.core.model.WidgetUpdatePublisher

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetPreferencesEntryPoint {
    fun widgetPreferences(): WidgetPreferences

    fun widgetUpdatePublisher(): WidgetUpdatePublisher
}

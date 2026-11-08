package dev.involvex.kiln.feature.apps.di

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.involvex.kiln.feature.apps.export.ApkExporter

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ApkExporterEntryPoint {
    fun apkExporter(): ApkExporter
}

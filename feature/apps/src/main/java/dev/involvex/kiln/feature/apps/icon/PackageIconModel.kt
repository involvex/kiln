package dev.involvex.kiln.feature.apps.icon

/**
 * Coil model for installed-app icons. Cache key uses [packageName] + [lastUpdateTime].
 */
data class PackageIconModel(val packageName: String, val lastUpdateTime: Long)

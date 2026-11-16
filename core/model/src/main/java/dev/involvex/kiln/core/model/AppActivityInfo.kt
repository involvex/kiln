package dev.involvex.kiln.core.model

/**
 * Exported activity declared by an installed package (Tier NONE listing).
 */
data class AppActivityInfo(
    val packageName: String,
    val className: String,
    val label: String,
    val exported: Boolean,
    val launchMode: Int,
    val taskAffinity: String?,
    val activityFlags: Int,
)

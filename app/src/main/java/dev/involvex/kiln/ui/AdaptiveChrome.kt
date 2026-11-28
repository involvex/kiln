package dev.involvex.kiln.ui

import androidx.window.core.layout.WindowSizeClass

/**
 * Compact phones keep the bottom [androidx.compose.material3.NavigationBar].
 * Medium+ width (tablet / unfolded foldable / landscape) uses [androidx.compose.material3.NavigationRail].
 */
fun useNavigationRail(widthDp: Int): Boolean = widthDp >= WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND

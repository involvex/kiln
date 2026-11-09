package dev.involvex.kiln.core.designsystem.scroll

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

/**
 * Bottom-nav reselect signal: increment [generation] when the user taps the already-selected tab.
 * Screens observe it and scroll their primary list / column to the top.
 */
class ScrollToTopSignal {
    var generation by mutableIntStateOf(0)
        private set

    fun request() {
        generation++
    }
}

val LocalScrollToTopSignal = compositionLocalOf<ScrollToTopSignal?> { null }

@Composable
fun ScrollToTopEffect(listState: LazyListState) {
    val signal = LocalScrollToTopSignal.current
    val generation = signal?.generation ?: 0
    LaunchedEffect(generation) {
        if (generation == 0) return@LaunchedEffect
        listState.animateScrollToItem(0)
    }
}

@Composable
fun ScrollToTopEffect(scrollState: ScrollState) {
    val signal = LocalScrollToTopSignal.current
    val generation = signal?.generation ?: 0
    LaunchedEffect(generation) {
        if (generation == 0) return@LaunchedEffect
        scrollState.animateScrollTo(0)
    }
}

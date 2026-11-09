package dev.involvex.kiln.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList

/**
 * Official Navigation 3 bottom-bar recipe: one back stack per top-level tab,
 * exposed as a flattened stack for [androidx.navigation3.ui.NavDisplay].
 */
class TopLevelBackStack<T : Any>(startKey: T) {
    private var topLevelStacks: LinkedHashMap<T, SnapshotStateList<T>> = linkedMapOf(
        startKey to mutableStateListOf(startKey),
    )

    var topLevelKey by mutableStateOf(startKey)
        private set

    val backStack = mutableStateListOf(startKey)

    private fun updateBackStack() {
        backStack.apply {
            clear()
            addAll(topLevelStacks.flatMap { it.value })
        }
    }

    /**
     * Selects [key] as the active top-level tab.
     *
     * @return `true` when the tab was already selected and already at its root
     * (caller should scroll that tab’s content to the top). Nested destinations
     * on a reselected tab are popped first; that returns `false`.
     */
    fun addTopLevel(key: T): Boolean {
        val existing = topLevelStacks[key]
        if (existing == null) {
            topLevelStacks[key] = mutableStateListOf(key)
            topLevelKey = key
            updateBackStack()
            return false
        }

        val alreadySelected = key == topLevelKey
        // Move this stack to the end so [keys.last] stays the active tab.
        topLevelStacks.apply {
            remove(key)?.let { put(key, it) }
        }
        topLevelKey = key

        if (alreadySelected && existing.size > 1) {
            while (existing.size > 1) {
                // Prefer removeAt over List.removeLast() (API 35+ / NewApi on minSdk 26).
                existing.removeAt(existing.lastIndex)
            }
            updateBackStack()
            return false
        }

        updateBackStack()
        return alreadySelected
    }

    fun add(key: T) {
        topLevelStacks[topLevelKey]?.add(key)
        updateBackStack()
    }

    fun removeLast() {
        val removedKey = topLevelStacks[topLevelKey]?.removeLastOrNull()
        topLevelStacks.remove(removedKey)
        topLevelKey = topLevelStacks.keys.last()
        updateBackStack()
    }
}

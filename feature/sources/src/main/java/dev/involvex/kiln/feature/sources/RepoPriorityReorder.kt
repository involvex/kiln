package dev.involvex.kiln.feature.sources

/**
 * Pure helper: move [moveId] up (-1) or down (+1) in [orderedIds] and renumber priorities 0..n-1.
 * Returns null when the move is a no-op (already at edge or id missing).
 */
object RepoPriorityReorder {
    fun afterMove(orderedIds: List<String>, moveId: String, delta: Int): List<Pair<String, Int>>? {
        val index = orderedIds.indexOf(moveId)
        if (index < 0) return null
        val target = index + delta
        if (target !in orderedIds.indices) return null
        val mutable = orderedIds.toMutableList()
        val item = mutable.removeAt(index)
        mutable.add(target, item)
        return mutable.mapIndexed { i, id -> id to i }
    }
}

package dev.involvex.kiln.core.model

/** Kind of measured storage consumer shown in the Storage treemap. */
enum class StorageTreemapKind {
    APP,
    LEFTOVERS,
    ORPHANS,
    LARGE_FILES,
}

/** One leaf in the Storage treemap (app or scanned aggregate). */
data class StorageTreemapLeaf(val id: String, val label: String, val bytes: Long, val kind: StorageTreemapKind)

/** Laid-out rectangle for a [StorageTreemapLeaf] in canvas coordinates. */
data class StorageTreemapRect(val leaf: StorageTreemapLeaf, val x: Float, val y: Float, val width: Float, val height: Float) {
    val area: Float get() = width * height
}

/**
 * Builds treemap leaves from measured Storage data only (no invented categories).
 * Aggregate leaves use caller-supplied display labels (UI strings).
 */
object StorageTreemapLeaves {
    const val DEFAULT_APP_LIMIT = 12
    const val ID_LEFTOVERS = "aggregate:leftovers"
    const val ID_ORPHANS = "aggregate:orphans"
    const val ID_LARGE_FILES = "aggregate:large_files"

    fun from(
        apps: List<AppStorageStats>,
        leftoversBytes: Long = 0L,
        orphansBytes: Long = 0L,
        largeFilesBytes: Long = 0L,
        leftoversLabel: String = "APK leftovers",
        orphansLabel: String = "Orphaned Android/data",
        largeFilesLabel: String = "Large files",
        appLimit: Int = DEFAULT_APP_LIMIT,
    ): List<StorageTreemapLeaf> {
        val leaves = mutableListOf<StorageTreemapLeaf>()
        StoragePhase2Filter.topAppsBySize(apps, appLimit).forEach { app ->
            if (app.totalBytes > 0L) {
                leaves += StorageTreemapLeaf(
                    id = "app:${app.packageName}",
                    label = app.label,
                    bytes = app.totalBytes,
                    kind = StorageTreemapKind.APP,
                )
            }
        }
        if (leftoversBytes > 0L) {
            leaves += StorageTreemapLeaf(
                id = ID_LEFTOVERS,
                label = leftoversLabel,
                bytes = leftoversBytes,
                kind = StorageTreemapKind.LEFTOVERS,
            )
        }
        if (orphansBytes > 0L) {
            leaves += StorageTreemapLeaf(
                id = ID_ORPHANS,
                label = orphansLabel,
                bytes = orphansBytes,
                kind = StorageTreemapKind.ORPHANS,
            )
        }
        if (largeFilesBytes > 0L) {
            leaves += StorageTreemapLeaf(
                id = ID_LARGE_FILES,
                label = largeFilesLabel,
                bytes = largeFilesBytes,
                kind = StorageTreemapKind.LARGE_FILES,
            )
        }
        return leaves.sortedByDescending { it.bytes }
    }
}

/**
 * Squarified treemap layout (Bruls et al.) — pure geometry, unit-tested.
 * Coordinates origin top-left; areas are proportional to [StorageTreemapLeaf.bytes].
 */
object StorageTreemapLayout {
    fun layout(leaves: List<StorageTreemapLeaf>, width: Float, height: Float): List<StorageTreemapRect> {
        if (width <= 0f || height <= 0f) return emptyList()
        val positive = leaves.filter { it.bytes > 0L }.sortedByDescending { it.bytes }
        if (positive.isEmpty()) return emptyList()
        val totalBytes = positive.sumOf { it.bytes }.toDouble()
        if (totalBytes <= 0.0) return emptyList()
        val totalArea = width * height
        val remaining = positive.map { AreaItem(it, (it.bytes.toDouble() / totalBytes * totalArea).toFloat()) }
            .toMutableList()
        val out = mutableListOf<StorageTreemapRect>()
        squarify(remaining, mutableListOf(), Container(0f, 0f, width, height), out)
        return out
    }

    private data class AreaItem(val leaf: StorageTreemapLeaf, val area: Float)

    private data class Container(var x: Float, var y: Float, var w: Float, var h: Float) {
        fun shortestSide(): Float = minOf(w, h)
    }

    private fun squarify(
        remaining: MutableList<AreaItem>,
        row: MutableList<AreaItem>,
        box: Container,
        out: MutableList<StorageTreemapRect>,
    ) {
        if (remaining.isEmpty()) {
            if (row.isNotEmpty()) {
                layoutRow(row, box, out)
            }
            return
        }
        val next = remaining.first()
        val side = box.shortestSide()
        if (row.isEmpty() || worst(row, side) >= worst(row + next, side)) {
            remaining.removeAt(0)
            row += next
            if (remaining.isEmpty()) {
                layoutRow(row, box, out)
            } else {
                squarify(remaining, row, box, out)
            }
        } else {
            layoutRow(row, box, out)
            squarify(remaining, mutableListOf(), box, out)
        }
    }

    private fun worst(row: List<AreaItem>, side: Float): Float {
        if (row.isEmpty() || side <= 0f) return Float.MAX_VALUE
        val s = row.sumOf { it.area.toDouble() }.toFloat()
        if (s <= 0f) return Float.MAX_VALUE
        val maxA = row.maxOf { it.area }
        val minA = row.minOf { it.area }
        val sideSq = side * side
        return maxOf((sideSq * maxA) / (s * s), (s * s) / (sideSq * minA))
    }

    private fun layoutRow(row: MutableList<AreaItem>, box: Container, out: MutableList<StorageTreemapRect>) {
        if (row.isEmpty()) return
        val rowArea = row.sumOf { it.area.toDouble() }.toFloat()
        if (rowArea <= 0f) return
        val horizontal = box.w >= box.h
        if (horizontal) {
            val rowHeight = (rowArea / box.w).coerceAtMost(box.h)
            var x = box.x
            row.forEach { item ->
                val w = (item.area / rowHeight).coerceAtMost(box.w - (x - box.x) + 0.0001f)
                out += StorageTreemapRect(item.leaf, x, box.y, w.coerceAtLeast(0f), rowHeight)
                x += w
            }
            box.y += rowHeight
            box.h -= rowHeight
        } else {
            val rowWidth = (rowArea / box.h).coerceAtMost(box.w)
            var y = box.y
            row.forEach { item ->
                val h = (item.area / rowWidth).coerceAtMost(box.h - (y - box.y) + 0.0001f)
                out += StorageTreemapRect(item.leaf, box.x, y, rowWidth, h.coerceAtLeast(0f))
                y += h
            }
            box.x += rowWidth
            box.w -= rowWidth
        }
        row.clear()
    }
}

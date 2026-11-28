package dev.involvex.kiln.core.model

import kotlin.math.abs
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StorageTreemapTest {
    @Test
    fun leaves_from_apps_and_aggregates_sorted_descending() {
        val apps = listOf(
            app("a", "A", 100),
            app("b", "B", 300),
            app("c", "C", 0),
        )
        val leaves = StorageTreemapLeaves.from(
            apps = apps,
            leftoversBytes = 50,
            orphansBytes = 0,
            largeFilesBytes = 200,
            leftoversLabel = "Leftovers",
            largeFilesLabel = "Large",
            appLimit = 8,
        )
        assertEquals(
            listOf("app:b", "aggregate:large_files", "app:a", "aggregate:leftovers"),
            leaves.map { it.id },
        )
        assertEquals(StorageTreemapKind.APP, leaves.first().kind)
        assertEquals(StorageTreemapKind.LARGE_FILES, leaves[1].kind)
    }

    @Test
    fun layout_empty_or_zero_canvas_returns_empty() {
        val leaf = StorageTreemapLeaf("a", "A", 10, StorageTreemapKind.APP)
        assertTrue(StorageTreemapLayout.layout(listOf(leaf), 0f, 100f).isEmpty())
        assertTrue(StorageTreemapLayout.layout(listOf(leaf), 100f, 0f).isEmpty())
        assertTrue(StorageTreemapLayout.layout(emptyList(), 100f, 100f).isEmpty())
        assertTrue(
            StorageTreemapLayout.layout(
                listOf(StorageTreemapLeaf("z", "Z", 0, StorageTreemapKind.APP)),
                100f,
                100f,
            ).isEmpty(),
        )
    }

    @Test
    fun layout_single_leaf_fills_canvas() {
        val leaf = StorageTreemapLeaf("only", "Only", 42, StorageTreemapKind.APP)
        val rects = StorageTreemapLayout.layout(listOf(leaf), 200f, 100f)
        assertEquals(1, rects.size)
        assertEquals(leaf.id, rects[0].leaf.id)
        assertEquals(200f, rects[0].width, 0.5f)
        assertEquals(100f, rects[0].height, 0.5f)
    }

    @Test
    fun layout_areas_proportional_to_bytes() {
        val leaves = listOf(
            StorageTreemapLeaf("big", "Big", 300, StorageTreemapKind.APP),
            StorageTreemapLeaf("mid", "Mid", 200, StorageTreemapKind.LEFTOVERS),
            StorageTreemapLeaf("small", "Small", 100, StorageTreemapKind.ORPHANS),
        )
        val width = 300f
        val height = 200f
        val rects = StorageTreemapLayout.layout(leaves, width, height)
        assertEquals(3, rects.size)
        val totalArea = width * height
        val totalBytes = 600.0
        rects.forEach { rect ->
            val expected = rect.leaf.bytes.toDouble() / totalBytes * totalArea
            assertTrue(
                abs(rect.area - expected) / expected < 0.05,
                "area ${rect.area} vs expected $expected for ${rect.leaf.id}",
            )
        }
        val covered = rects.sumOf { it.area.toDouble() }
        assertTrue(abs(covered - totalArea) / totalArea < 0.02, "covered=$covered total=$totalArea")
    }

    private fun app(pkg: String, label: String, total: Long): AppStorageStats = AppStorageStats(
        packageName = pkg,
        label = label,
        isSystem = false,
        appBytes = total,
        dataBytes = 0,
        cacheBytes = 0,
    )
}

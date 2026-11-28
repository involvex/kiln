package dev.involvex.kiln.feature.debloat.catalog

import dev.involvex.kiln.core.model.DebloatRating
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DebloatDbParserTest {
    @Test
    fun empty_packages_keeps_metadata() {
        val doc = DebloatDbParser.parse(
            """
            {
              "version": 1,
              "source": "kiln-empty",
              "notes": "No invented ratings.",
              "packages": []
            }
            """.trimIndent(),
        )
        assertEquals(1, doc.version)
        assertEquals("kiln-empty", doc.source)
        assertEquals("No invented ratings.", doc.notes)
        assertTrue(doc.entries.isEmpty())
        assertEquals(0, doc.packageCount)
    }

    @Test
    fun fixture_maps_ratings_and_defaults() {
        val doc = DebloatDbParser.parse(
            """
            {
              "version": 2,
              "source": "test-fixture",
              "packages": [
                {
                  "pkg": "com.example.safe",
                  "label": "Safe",
                  "vendor": "AOSP",
                  "category": "demo",
                  "rating": "RECOMMENDED",
                  "description": "Fixture only",
                  "breaks": ["nothing"]
                },
                {
                  "pkg": "com.example.weird",
                  "rating": "NOT_A_REAL_RATING"
                }
              ]
            }
            """.trimIndent(),
        )
        assertEquals(2, doc.packageCount)
        val safe = doc.entries.getValue("com.example.safe")
        assertEquals(DebloatRating.RECOMMENDED, safe.rating)
        assertEquals("Safe", safe.label)
        assertEquals(listOf("nothing"), safe.breaks)
        assertEquals(0, safe.minSdk)
        assertEquals(99, safe.maxSdk)
        assertEquals(DebloatRating.UNKNOWN, doc.entries.getValue("com.example.weird").rating)
    }

    @Test
    fun shipped_asset_shape_parses_empty() {
        // Mirrors feature/debloat assets — keep packages empty (UAD-NG GPL-3.0 not cleared).
        val doc = DebloatDbParser.parse(
            """
            {
              "version": 1,
              "source": "kiln-empty",
              "notes": "M68: UAD-NG uad_lists.json is GPL-3.0 — not cleared for Apache-2.0 Kiln (docs/reference.md). packages stay empty until an approved Apache-2.0/CC0/dual-licensed or Kiln-first-party curated source lands with NOTICE. Do not invent ratings. Unknown installed packages use UNKNOWN and require expert mode.",
              "packages": []
            }
            """.trimIndent(),
        )
        assertEquals("kiln-empty", doc.source)
        assertTrue(doc.entries.isEmpty())
        assertEquals(0, doc.packageCount)
    }
}

package dev.involvex.kiln.feature.sources.exodus

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ExodusSearchParserTest {
    @Test
    fun parseSearch_picksLatestReportTrackers() {
        val body = """
            {
              "com.example.app": {
                "name": "Example",
                "reports": [
                  { "id": 1, "trackers": [10], "version": "1.0" },
                  { "id": 9, "trackers": [27, 69], "version": "2.0" }
                ]
              }
            }
        """.trimIndent()
        val parsed = ExodusSearchParser.parseSearch("com.example.app", body)!!
        assertEquals(listOf(27, 69), parsed.trackerIds)
        assertEquals(2, parsed.reportCount)
    }

    @Test
    fun parseSearch_missingPackage_returnsNull() {
        assertNull(ExodusSearchParser.parseSearch("com.missing", """{"other.app":{"reports":[]}}"""))
    }

    @Test
    fun parseTrackersMap_readsNames() {
        val body = """
            {
              "trackers": {
                "27": { "name": "Google Analytics", "categories": ["Analytics"] },
                "69": { "name": "Facebook Places", "categories": ["Ads"] }
              }
            }
        """.trimIndent()
        val map = ExodusSearchParser.parseTrackersMap(body)
        assertEquals("Google Analytics", map[27])
        assertEquals("Facebook Places", map[69])
    }
}

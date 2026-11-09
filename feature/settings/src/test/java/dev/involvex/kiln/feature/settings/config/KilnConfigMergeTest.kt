package dev.involvex.kiln.feature.settings.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class KilnConfigMergeTest {
    @Test
    fun counts_new_and_updated_repos() {
        val existing = setOf("fdroid", "izzy")
        val incoming = listOf(
            KilnConfigRepoV1(id = "fdroid", name = "F-Droid", address = "https://f-droid.org/repo"),
            KilnConfigRepoV1(id = "custom", name = "Custom", address = "https://example.com/repo"),
            KilnConfigRepoV1(id = "", name = "Bad", address = "https://x"),
        )
        val (neu, updated) = KilnConfigMerge.countRepoChanges(existing, incoming)
        assertEquals(1, neu)
        assertEquals(1, updated)
    }

    @Test
    fun lists_present_prefs_sections() {
        val sections = KilnConfigMerge.prefsSectionsPresent(
            KilnConfigPreferencesV1(
                automation = KilnConfigAutomationV1(enabled = true),
                termux = KilnConfigTermuxV1(enabled = false),
            ),
        )
        assertEquals(listOf("automation", "termux"), sections)
    }
}

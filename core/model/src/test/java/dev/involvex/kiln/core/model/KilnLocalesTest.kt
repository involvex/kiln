package dev.involvex.kiln.core.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class KilnLocalesTest {
    @Test
    fun desiredTags() {
        assertEquals("", KilnLocales.desiredLanguageTags(KilnLanguage.SYSTEM))
        assertEquals("en", KilnLocales.desiredLanguageTags(KilnLanguage.ENGLISH))
        assertEquals("de", KilnLocales.desiredLanguageTags(KilnLanguage.GERMAN))
    }

    @Test
    fun systemSatisfiedOnlyWhenEmpty() {
        assertTrue(KilnLocales.isSatisfied(KilnLanguage.SYSTEM, ""))
        assertFalse(KilnLocales.isSatisfied(KilnLanguage.SYSTEM, "en-US"))
    }

    @Test
    fun englishMatchesRegionVariants() {
        assertTrue(KilnLocales.isSatisfied(KilnLanguage.ENGLISH, "en"))
        assertTrue(KilnLocales.isSatisfied(KilnLanguage.ENGLISH, "en-US"))
        assertTrue(KilnLocales.isSatisfied(KilnLanguage.ENGLISH, "en-GB"))
        assertFalse(KilnLocales.isSatisfied(KilnLanguage.ENGLISH, "de"))
        assertFalse(KilnLocales.isSatisfied(KilnLanguage.ENGLISH, ""))
    }

    @Test
    fun germanMatchesRegionVariants() {
        assertTrue(KilnLocales.isSatisfied(KilnLanguage.GERMAN, "de"))
        assertTrue(KilnLocales.isSatisfied(KilnLanguage.GERMAN, "de-DE"))
        assertFalse(KilnLocales.isSatisfied(KilnLanguage.GERMAN, "en-US"))
    }
}

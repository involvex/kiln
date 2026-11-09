package dev.involvex.kiln.core.model

import java.util.Locale

/**
 * Locale apply helpers shared by Application / Activity.
 *
 * HyperOS often reports stored locales as `en-US` / `de-DE` while Kiln requests
 * `en` / `de`. Exact string equality then re-calls setApplicationLocales forever
 * and recreates MainActivity (full-screen black flash).
 */
object KilnLocales {
    /** BCP-47 tags to request, or empty string for follow-system (empty LocaleList). */
    fun desiredLanguageTags(language: KilnLanguage): String = when (language) {
        KilnLanguage.SYSTEM -> ""
        KilnLanguage.ENGLISH -> "en"
        KilnLanguage.GERMAN -> "de"
    }

    /**
     * Whether [currentLanguageTags] (from `LocaleListCompat.toLanguageTags()`)
     * already matches the user’s [language] preference.
     */
    fun isSatisfied(language: KilnLanguage, currentLanguageTags: String): Boolean {
        val current = currentLanguageTags.trim()
        return when (language) {
            KilnLanguage.SYSTEM -> current.isEmpty()
            KilnLanguage.ENGLISH -> primaryLanguage(current) == "en"
            KilnLanguage.GERMAN -> primaryLanguage(current) == "de"
        }
    }

    private fun primaryLanguage(tags: String): String {
        if (tags.isEmpty()) return ""
        val first = tags.substringBefore(',')
        return Locale.forLanguageTag(first).language.lowercase(Locale.ROOT)
    }
}

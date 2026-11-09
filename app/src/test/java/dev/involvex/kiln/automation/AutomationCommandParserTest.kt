package dev.involvex.kiln.automation

import dev.involvex.kiln.core.model.AutomationApi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AutomationCommandParserTest {
    @Test
    fun checkUpdatesParses() {
        val command = AutomationCommandParser.parse(
            AutomationApi.ACTION_CHECK_UPDATES,
            AutomationExtras(),
        ).getOrThrow()
        assertEquals(AutomationCommand.CheckUpdates, command)
    }

    @Test
    fun installUpdateRequiresPackage() {
        assertTrue(
            AutomationCommandParser.parse(
                AutomationApi.ACTION_INSTALL_UPDATE,
                AutomationExtras(),
            ).isFailure,
        )
        val command = AutomationCommandParser.parse(
            AutomationApi.ACTION_INSTALL_UPDATE,
            AutomationExtras(packageName = "com.example.app"),
        ).getOrThrow() as AutomationCommand.InstallUpdate
        assertEquals("com.example.app", command.packageName)
    }

    @Test
    fun installFromUrlRequiresHttps() {
        val http = AutomationCommandParser.parse(
            AutomationApi.ACTION_INSTALL_FROM_URL,
            AutomationExtras(
                packageName = "com.example.app",
                url = "http://example.com/a.apk",
            ),
        )
        assertEquals("url_must_be_https", http.exceptionOrNull()?.message)

        val command = AutomationCommandParser.parse(
            AutomationApi.ACTION_INSTALL_FROM_URL,
            AutomationExtras(
                packageName = "com.example.app",
                url = "https://example.com/a.apk",
                versionName = "1.2.3",
                versionCode = 12L,
                sha256 = "abc",
            ),
        ).getOrThrow() as AutomationCommand.InstallFromUrl
        assertEquals("com.example.app", command.packageName)
        assertEquals("https://example.com/a.apk", command.url)
        assertEquals("1.2.3", command.versionName)
        assertEquals(12L, command.versionCode)
        assertEquals("abc", command.sha256)
    }

    @Test
    fun cleanCacheAllowsMissingPackage() {
        val all = AutomationCommandParser.parse(
            AutomationApi.ACTION_CLEAN_CACHE,
            AutomationExtras(),
        ).getOrThrow() as AutomationCommand.CleanCache
        assertEquals(null, all.packageName)

        val one = AutomationCommandParser.parse(
            AutomationApi.ACTION_CLEAN_CACHE,
            AutomationExtras(packageName = "com.example.app"),
        ).getOrThrow() as AutomationCommand.CleanCache
        assertEquals("com.example.app", one.packageName)
    }

    @Test
    fun unknownActionFails() {
        val result = AutomationCommandParser.parse("dev.involvex.kiln.action.NOPE", AutomationExtras())
        assertEquals("unknown_action", result.exceptionOrNull()?.message)
    }

    @Test
    fun isHttpsUrlRejectsNonHttps() {
        assertTrue(AutomationCommandParser.isHttpsUrl("https://cdn.example.com/app.apk"))
        assertFalse(AutomationCommandParser.isHttpsUrl("http://cdn.example.com/app.apk"))
        assertFalse(AutomationCommandParser.isHttpsUrl("https://"))
        assertFalse(AutomationCommandParser.isHttpsUrl("ftp://example.com/a.apk"))
        assertFalse(AutomationCommandParser.isHttpsUrl(""))
    }
}

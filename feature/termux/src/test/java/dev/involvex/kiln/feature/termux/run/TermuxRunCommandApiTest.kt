package dev.involvex.kiln.feature.termux.run

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TermuxRunCommandApiTest {
    @Test
    fun officialKeysMatchWikiExamples() {
        val id = "com.termux"
        assertEquals("com.termux.RUN_COMMAND", TermuxRunCommandApi.action(id))
        assertEquals("com.termux.app.RunCommandService", TermuxRunCommandApi.serviceClassName(id))
        assertEquals("com.termux.permission.RUN_COMMAND", TermuxRunCommandApi.permission(id))
        assertEquals("com.termux.RUN_COMMAND_PATH", TermuxRunCommandApi.extraCommandPath(id))
        assertEquals("com.termux.RUN_COMMAND_ARGUMENTS", TermuxRunCommandApi.extraArguments(id))
        assertEquals("com.termux.RUN_COMMAND_WORKDIR", TermuxRunCommandApi.extraWorkdir(id))
        assertEquals("com.termux.RUN_COMMAND_BACKGROUND", TermuxRunCommandApi.extraBackground(id))
        assertEquals(
            "com.termux.RUN_COMMAND_PENDING_INTENT",
            TermuxRunCommandApi.extraPendingIntent(id),
        )
        assertEquals("result", TermuxRunCommandApi.EXTRA_PLUGIN_RESULT_BUNDLE)
        assertEquals("stdout", TermuxRunCommandApi.EXTRA_STDOUT)
        assertEquals("exitCode", TermuxRunCommandApi.EXTRA_EXIT_CODE)
    }
}

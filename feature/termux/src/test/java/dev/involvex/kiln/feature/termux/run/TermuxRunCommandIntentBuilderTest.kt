package dev.involvex.kiln.feature.termux.run

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TermuxRunCommandIntentBuilderTest {
    @Test
    fun specUsesOfficialWikiKeysForComTermux() {
        val request = TermuxRunCommandRequest(
            packageId = "com.termux",
            commandPath = "/data/data/com.termux/files/usr/bin/echo",
            arguments = listOf("hello", "world"),
            workdir = "/data/data/com.termux/files/home",
            background = true,
            label = "test",
            executionId = 42,
        )
        val spec = TermuxRunCommandIntentBuilder.spec(request)

        assertEquals("com.termux", spec.packageId)
        assertEquals("com.termux.RUN_COMMAND", spec.action)
        assertEquals("com.termux.app.RunCommandService", spec.serviceClassName)
        assertEquals("/data/data/com.termux/files/usr/bin/echo", spec.commandPath)
        assertEquals(listOf("hello", "world"), spec.arguments)
        assertEquals("/data/data/com.termux/files/home", spec.workdir)
        assertTrue(spec.background)
        assertEquals("test", spec.label)
    }

    @Test
    fun forkPackageIdPrefixesActionAndServiceClass() {
        val fork = "dev.involvex.termux"
        val request = TermuxRunCommandRequest(
            packageId = fork,
            commandPath = TermuxPaths.executable(fork, "uname"),
            arguments = emptyList(),
            background = false,
            executionId = 7,
        )
        val spec = TermuxRunCommandIntentBuilder.spec(request)

        assertEquals("$fork.RUN_COMMAND", spec.action)
        assertEquals("$fork.app.RunCommandService", spec.serviceClassName)
        assertEquals("/data/data/$fork/files/usr/bin/uname", spec.commandPath)
        assertEquals("/data/data/$fork/files/home", spec.workdir)
    }

    @Test
    fun defaultsWorkdirToHomeWhenOmitted() {
        val request = TermuxRunCommandRequest(
            packageId = "com.termux",
            commandPath = "$PREFIX_ECHO",
            executionId = 1,
        )
        val spec = TermuxRunCommandIntentBuilder.spec(request)
        assertEquals(TermuxPaths.homeDir("com.termux"), spec.workdir)
    }

    @Test
    fun blankPackageOrPathRejected() {
        assertThrows<IllegalArgumentException> {
            TermuxRunCommandIntentBuilder.spec(
                TermuxRunCommandRequest(packageId = " ", commandPath = "/bin/echo", executionId = 1),
            )
        }
        assertThrows<IllegalArgumentException> {
            TermuxRunCommandIntentBuilder.spec(
                TermuxRunCommandRequest(packageId = "com.termux", commandPath = "", executionId = 1),
            )
        }
    }

    @Test
    fun echoSmokeTestUsesPrefixEcho() {
        val request = TermuxRunCommandIntentBuilder.echoSmokeTest("com.termux", 99)
        assertEquals("/data/data/com.termux/files/usr/bin/echo", request.commandPath)
        assertEquals(listOf("kiln-termux-ok"), request.arguments)
        assertTrue(request.background)
        assertEquals(99, request.executionId)
    }

    private companion object {
        const val PREFIX_ECHO = "/data/data/com.termux/files/usr/bin/echo"
    }
}

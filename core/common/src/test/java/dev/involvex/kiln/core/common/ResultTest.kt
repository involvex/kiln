package dev.involvex.kiln.core.common

import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ResultTest {
    @Test
    fun `success maps value`() {
        val result = Result.success(2).map { it * 3 }
        assertEquals(6, result.getOrNull())
        assertTrue(result.isSuccess)
    }

    @Test
    fun `failure preserves message`() {
        val cause = mockk<Throwable>()
        every { cause.message } returns "boom"
        val result = Result.failure("unavailable", cause)
        assertTrue(result.isFailure)
        assertEquals("unavailable", (result as Result.Failure).message)
        assertEquals("boom", result.exceptionOrNull()?.message)
    }

    @Test
    fun `state flow emits with turbine`() = runTest {
        val flow = MutableStateFlow(0)
        flow.test {
            assertEquals(0, awaitItem())
            flow.update { it + 1 }
            assertEquals(1, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}

package dev.involvex.kiln.core.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AndroidUserListParserTest {
    @Test
    fun parse_single_owner_user() {
        val out = AndroidUserListParser.parse(
            """
            Users:
            	UserInfo{0:Owner:13} running
            """.trimIndent(),
        )
        assertEquals(1, out.size)
        assertEquals(0, out[0].id)
        assertEquals("Owner", out[0].name)
        assertTrue(out[0].running)
    }

    @Test
    fun parse_multiple_users_including_stopped() {
        val out = AndroidUserListParser.parse(
            """
            Users:
            	UserInfo{0:Owner:c13} running
            	UserInfo{10:Work:30}
            	UserInfo{999:Second space:400}
            """.trimIndent(),
        )
        assertEquals(3, out.size)
        assertEquals(10, out[1].id)
        assertEquals("Work", out[1].name)
        assertFalse(out[1].running)
        assertEquals(999, out[2].id)
        assertEquals("Second space", out[2].name)
    }

    @Test
    fun parse_ignores_noise() {
        assertTrue(AndroidUserListParser.parse("garbage\n").isEmpty())
    }
}

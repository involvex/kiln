package dev.involvex.kiln.feature.sources.github

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GitHubVersionCodeTest {
    @Test
    fun parses_semver_tag() {
        assertEquals(1_002_003L, GitHubVersionCode.parse("v1.2.3"))
    }

    @Test
    fun empty_tag_is_zero() {
        assertEquals(0L, GitHubVersionCode.parse("nightly"))
    }
}

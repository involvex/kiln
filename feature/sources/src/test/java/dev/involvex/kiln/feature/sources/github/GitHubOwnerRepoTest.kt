package dev.involvex.kiln.feature.sources.github

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class GitHubOwnerRepoTest {
    @Test
    fun parses_slash_form() {
        assertEquals("involvex/kiln", GitHubOwnerRepo.parse("involvex/kiln"))
    }

    @Test
    fun parses_url() {
        assertEquals(
            "involvex/kiln",
            GitHubOwnerRepo.parse("https://github.com/involvex/kiln"),
        )
    }

    @Test
    fun rejects_garbage() {
        assertNull(GitHubOwnerRepo.parse("not a repo"))
        assertNull(GitHubOwnerRepo.parse(""))
    }

    @Test
    fun provisional_package_name() {
        assertEquals(
            "github.involvex.kiln",
            GitHubOwnerRepo.provisionalPackageName("involvex/kiln"),
        )
    }
}

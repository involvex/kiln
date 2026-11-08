package dev.involvex.kiln.feature.sources.github

import dev.involvex.kiln.feature.sources.obtainium.ObtainiumImport
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class GithubShareParseTest {
    @Test
    fun extracts_owner_repo_from_share_text() {
        val text = "Check this out: https://github.com/involvex/kiln — nice app"
        assertEquals("involvex/kiln", ObtainiumImport.githubOwnerRepoFromUrl(text))
    }

    @Test
    fun rejects_non_github() {
        assertNull(ObtainiumImport.githubOwnerRepoFromUrl("https://gitlab.com/foo/bar"))
    }
}

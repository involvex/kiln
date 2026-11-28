package dev.involvex.kiln.feature.sources.forge

import dev.involvex.kiln.core.model.ReleaseForge
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ForgeUrlParseTest {
    @Test
    fun bare_slash_defaults_to_github() {
        assertEquals(ForgeRef(ReleaseForge.GITHUB, "involvex/kiln"), ForgeUrlParse.parse("involvex/kiln"))
    }

    @Test
    fun parses_github_url() {
        assertEquals(
            ForgeRef(ReleaseForge.GITHUB, "involvex/kiln"),
            ForgeUrlParse.parse("https://github.com/involvex/kiln"),
        )
    }

    @Test
    fun parses_gitlab_url_in_share_text() {
        val text = "see https://gitlab.com/fdroid/fdroiddata/-/tree/master please"
        // path stops at first segment after owner — tree path not part of owner/repo
        assertEquals(
            ForgeRef(ReleaseForge.GITLAB, "fdroid/fdroiddata"),
            ForgeUrlParse.parse(text),
        )
    }

    @Test
    fun parses_codeberg_url() {
        assertEquals(
            ForgeRef(ReleaseForge.CODEBERG, "forgejo/forgejo"),
            ForgeUrlParse.parse("https://codeberg.org/forgejo/forgejo.git"),
        )
    }

    @Test
    fun parseUrl_rejects_bare_slash() {
        assertNull(ForgeUrlParse.parseUrl("owner/repo"))
    }

    @Test
    fun provisional_package_prefixes() {
        assertEquals(
            "gitlab.group.app",
            ForgeRef(ReleaseForge.GITLAB, "group/app").provisionalPackageName(),
        )
        assertEquals(
            "codeberg.org.pkg",
            ForgeRef(ReleaseForge.CODEBERG, "org/pkg").provisionalPackageName(),
        )
    }
}

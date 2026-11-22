package dev.involvex.kiln.feature.sources

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VersionCompareTest {
    @Test
    fun `versionCode wins`() {
        assertTrue(isRemoteNewer(10, "1.0", 11, "0.9"))
        assertFalse(isRemoteNewer(11, "0.9", 10, "2.0"))
    }

    @Test
    fun `falls back to versionName`() {
        assertTrue(isRemoteNewer(0, "1.0.0", 0, "1.0.1"))
        assertFalse(isRemoteNewer(0, "2.0", 0, "1.9"))
    }

    @Test
    fun `github tag newer than older chromium-style name`() {
        assertTrue(
            isGitHubRemoteNewer(
                installedCode = 1_580_812_742L,
                installedName = "157.0.8103.78",
                remoteCode = 158_000_999_042L,
                remoteName = "v158.0.8127.42",
            ),
        )
    }

    @Test
    fun `github same version name is not newer`() {
        assertFalse(
            isGitHubRemoteNewer(
                installedCode = 1L,
                installedName = "158.0.8127.42",
                remoteCode = 999_999_999L,
                remoteName = "v158.0.8127.42",
            ),
        )
    }

    @Test
    fun `github tag with larger build segment is newer despite clamp`() {
        // Folded tag code clamps 8127 and 8200 both to 999; name compare still sees the update.
        assertTrue(
            isGitHubRemoteNewer(
                installedCode = 1L,
                installedName = "158.0.8127.42",
                remoteCode = 158_000_999_005L,
                remoteName = "v158.0.8200.5",
            ),
        )
    }

    @Test
    fun `github falls back to versionCode when installed name missing`() {
        assertTrue(isGitHubRemoteNewer(10, null, 11, "v2.0"))
        assertFalse(isGitHubRemoteNewer(11, null, 10, "v2.0"))
    }
}

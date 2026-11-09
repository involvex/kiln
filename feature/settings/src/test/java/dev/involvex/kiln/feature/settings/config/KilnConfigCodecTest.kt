package dev.involvex.kiln.feature.settings.config

import dev.involvex.kiln.core.common.Result
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class KilnConfigCodecTest {
    @Test
    fun round_trip_preserves_repos_and_managed_apps() {
        val original = KilnConfigV1(
            schemaVersion = 1,
            exportedAtEpochMs = 1_700_000_000_000L,
            kilnVersionName = "0.2.2",
            kilnVersionCode = 5,
            repos = listOf(
                KilnConfigRepoV1(
                    id = "fdroid",
                    name = "F-Droid",
                    address = "https://f-droid.org/repo",
                    fingerprint = "abc",
                    enabled = true,
                    priority = 0,
                ),
            ),
            managedApps = listOf(
                KilnConfigManagedAppV1(
                    packageName = "com.example.app",
                    sourceId = "github:owner/repo",
                    githubOwnerRepo = "owner/repo",
                    watchOnly = true,
                ),
            ),
            preferences = KilnConfigPreferencesV1(
                automation = KilnConfigAutomationV1(enabled = true),
            ),
            hints = KilnConfigHintsV1(githubPatWasConfigured = true),
        )
        val encoded = KilnConfigCodec.encode(original)
        assertFalse(KilnConfigCodec.containsForbiddenSecretKeys(encoded))
        assertFalse(encoded.contains("\"pat\"", ignoreCase = true))
        assertFalse(encoded.contains("\"token\"", ignoreCase = true))
        when (val decoded = KilnConfigCodec.decode(encoded)) {
            is Result.Failure -> error(decoded.message)

            is Result.Success -> {
                assertEquals(original.repos, decoded.value.repos)
                assertEquals(original.managedApps, decoded.value.managedApps)
                assertEquals(true, decoded.value.preferences.automation?.enabled)
                assertTrue(decoded.value.hints.githubPatWasConfigured)
            }
        }
    }

    @Test
    fun rejects_wrong_schema_and_non_object() {
        assertTrue(KilnConfigCodec.decode("") is Result.Failure)
        assertTrue(KilnConfigCodec.decode("[]") is Result.Failure)
        val badSchema = """{"schemaVersion":99,"repos":[]}"""
        val result = KilnConfigCodec.decode(badSchema)
        assertTrue(result is Result.Failure)
        assertTrue((result as Result.Failure).message.contains("unsupported_schema"))
    }

    @Test
    fun secret_guard_flags_pat_key() {
        assertTrue(KilnConfigCodec.containsForbiddenSecretKeys("""{"pat":"secret"}"""))
        assertTrue(KilnConfigCodec.containsForbiddenSecretKeys("""{"token":"x"}"""))
        assertFalse(
            KilnConfigCodec.containsForbiddenSecretKeys(
                """{"hints":{"githubPatWasConfigured":true}}""",
            ),
        )
    }
}

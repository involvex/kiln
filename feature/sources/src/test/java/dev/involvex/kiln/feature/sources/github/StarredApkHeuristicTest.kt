package dev.involvex.kiln.feature.sources.github

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StarredApkHeuristicTest {
    @Test
    fun emptyAssets_isNoApk() {
        assertEquals(StarredApkHint.NO_APK, StarredApkHeuristic.fromReleaseAssets(emptyList()))
    }

    @Test
    fun apkAsset_isHasApk() {
        assertEquals(
            StarredApkHint.HAS_APK,
            StarredApkHeuristic.fromReleaseAssets(listOf("app-release.apk", "checksums.txt")),
        )
    }

    @Test
    fun apkIdSuffix_isHasApk() {
        assertEquals(
            StarredApkHint.HAS_APK,
            StarredApkHeuristic.fromReleaseAssets(listOf("foo.apk.id")),
        )
    }

    @Test
    fun nonApkOnly_isNoApk() {
        assertEquals(
            StarredApkHint.NO_APK,
            StarredApkHeuristic.fromReleaseAssets(listOf("source.tar.gz", "notes.md")),
        )
    }
}

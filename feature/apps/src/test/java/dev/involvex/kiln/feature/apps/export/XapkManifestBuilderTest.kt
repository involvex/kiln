package dev.involvex.kiln.feature.apps.export

import dev.involvex.kiln.core.model.ApkPathEntry
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class XapkManifestBuilderTest {
    @Test
    fun `builds version 2 manifest with string version code and split ids`() {
        val json = XapkManifestBuilder.buildJson(
            packageName = "dev.example.app",
            name = "Example",
            versionCode = 99,
            versionName = "9.9",
            minSdk = 26,
            targetSdk = 35,
            permissions = listOf("android.permission.INTERNET"),
            apkPaths = listOf(
                ApkPathEntry(path = "/data/app/base.apk", sizeBytes = 10, splitId = null),
                ApkPathEntry(path = "/data/app/split_config.arm64_v8a.apk", sizeBytes = 5, splitId = "config.arm64_v8a"),
            ),
        )
        assertTrue(json.contains("\"xapk_version\": 2"))
        assertTrue(json.contains("\"version_code\": \"99\""))
        assertTrue(json.contains("\"package_name\": \"dev.example.app\""))
        assertTrue(json.contains("\"id\":\"base\""))
        assertTrue(json.contains("\"id\":\"config.arm64_v8a\""))
        assertTrue(json.contains("\"total_size\": 15"))
        assertTrue(!json.contains("obb"))
    }
}

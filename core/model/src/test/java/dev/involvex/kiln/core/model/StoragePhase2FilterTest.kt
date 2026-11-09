package dev.involvex.kiln.core.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StoragePhase2FilterTest {
    @Test
    fun leftover_extension_match() {
        assertTrue(StoragePhase2Filter.isApkLeftoverName("app.apk"))
        assertTrue(StoragePhase2Filter.isApkLeftoverName("bundle.APKS"))
        assertTrue(StoragePhase2Filter.isApkLeftoverName("pack.xapk"))
        assertFalse(StoragePhase2Filter.isApkLeftoverName("notes.txt"))
        assertFalse(StoragePhase2Filter.isApkLeftoverName("apk"))
    }

    @Test
    fun orphan_dirs_exclude_installed() {
        val installed = setOf("com.keep.me", "com.also.here")
        val orphans = StoragePhase2Filter.orphanPackageDirs(
            listOf("com.keep.me", "com.orphan.app", "lost+found", "com.also.here"),
            installed,
        )
        assertEquals(listOf("com.orphan.app"), orphans)
    }

    @Test
    fun top_apps_by_size() {
        val apps = listOf(
            AppStorageStats("a", "A", false, 1, 0, 0),
            AppStorageStats("b", "B", false, 100, 0, 0),
            AppStorageStats("c", "C", false, 50, 0, 0),
        )
        assertEquals(listOf("b", "c"), StoragePhase2Filter.topAppsBySize(apps, 2).map { it.packageName })
    }

    @Test
    fun orphan_gate() {
        assertFalse(StorageOrphanGate.canAccessAndroidData(emptySet()))
        assertTrue(StorageOrphanGate.canAccessAndroidData(setOf(PrivilegeCapability.ACCESS_ANDROID_DATA)))
    }
}

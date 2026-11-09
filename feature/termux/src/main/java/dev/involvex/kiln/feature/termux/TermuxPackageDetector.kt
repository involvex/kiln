package dev.involvex.kiln.feature.termux

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.involvex.kiln.feature.termux.run.TermuxRunCommandApi
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Finds a Termux install on the device, including renamed forks.
 *
 * The official app is `com.termux`, but some builds (e.g. the one on the
 * test device) ship as `com.involvex.termux_app`. Kiln cannot guess that from
 * the manifest, so the user can ask Kiln to scan the installed apps and
 * pre-fill the package-id field with whatever it finds.
 *
 * Detection is a two-step check, not a name match alone:
 *  1. The package name must contain "termux" (case-insensitive) — a cheap filter.
 *  2. The package must expose a service advertising the package-scoped
 *     `RUN_COMMAND` action. This avoids matching an unrelated app that happens
 *     to contain the substring.
 *
 * The service component is resolved dynamically rather than derived from the
 * package id: Termux's RunCommandService class name is not always
 * `$packageId.app.RunCommandService` (the test-device fork ships it as
 * `com.invapp.app.RunCommandService`), so the launcher asks for the real
 * component instead of guessing.
 */
@Singleton
class TermuxPackageDetector @Inject constructor(@ApplicationContext private val context: Context) {

    data class Detected(val packageId: String)

    /**
     * Returns a Termux-like package that exposes RUN_COMMAND, or null.
     *
     * Add-ons such as `com.termux.boot` match the name filter but do not host
     * RunCommandService. Every candidate is tried, with official `com.termux`
     * preferred over forks.
     */
    fun detect(): Detected? {
        val installed = runCatching {
            context.packageManager.getInstalledApplications(PackageManager.GET_PERMISSIONS)
        }.getOrNull().orEmpty().mapNotNull { it.packageName }

        return termuxCandidates(installed)
            .firstOrNull { resolveServiceComponent(it) != null }
            ?.let { Detected(it) }
    }

    /** Resolves the actual RunCommandService component for [packageId], or null. */
    fun resolveServiceComponent(packageId: String): ComponentName? {
        val action = TermuxRunCommandApi.action(packageId)
        val matches = runCatching {
            context.packageManager.queryIntentServices(
                Intent(action).apply { setPackage(packageId) },
                PackageManager.GET_SERVICES,
            )
        }.getOrNull().orEmpty()
        return matches.firstOrNull()?.serviceInfo?.let { ComponentName(it.packageName, it.name) }
    }

    companion object {
        private const val OFFICIAL_PACKAGE_ID = "com.termux"

        /**
         * Termux-like package ids, official `com.termux` first, then the rest
         * in stable order. Callers still verify a RUN_COMMAND service exists.
         */
        @JvmStatic
        fun termuxCandidates(packageNames: Iterable<String>): List<String> = packageNames
            .filter { it.contains("termux", ignoreCase = true) }
            .distinct()
            .sortedWith(compareBy<String> { if (it == OFFICIAL_PACKAGE_ID) 0 else 1 }.thenBy { it })

        /** Preferred Termux-like package id, or null when none match the name filter. */
        @JvmStatic
        fun selectCandidate(packageNames: Iterable<String>): String? = termuxCandidates(packageNames).firstOrNull()
    }
}

package dev.involvex.kiln.convention

import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            // AGP 9: built-in Kotlin — do not apply org.jetbrains.kotlin.android.
            pluginManager.apply("com.android.application")
            configureAndroidApplication()
        }
    }
}

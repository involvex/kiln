plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.spotless)
    alias(libs.plugins.detekt)
}

spotless {
    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**")
        ktlint(libs.versions.ktlint.get())
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        target("**/*.gradle.kts")
        targetExclude("**/build/**")
        ktlint(libs.versions.ktlint.get())
        trimTrailingWhitespace()
        endWithNewline()
    }
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    parallel = true
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    source.setFrom(
        files(
            "app/src",
            "core/common/src",
            "core/model/src",
            "core/designsystem/src",
            "feature/apps/src",
            "feature/sources/src",
            "feature/storage/src",
            "feature/backup/src",
            "feature/settings/src",
        ),
    )
}

tasks.register("kilnCheck") {
    group = "verification"
    description = "Runs Spotless, detekt, lint, unit tests, and assembleDebug."
    dependsOn("spotlessCheck", "detekt", "lint", "testDebugUnitTest", "assembleDebug")
}

// Wire quality gates into check when Android modules exist.
subprojects {
    plugins.withId("com.android.library") {
        tasks.named("check").configure {
            dependsOn(rootProject.tasks.named("spotlessCheck"))
        }
    }
}

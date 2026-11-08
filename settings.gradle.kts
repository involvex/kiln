pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

// Pin vulnerable AGP/plugin classpath transitives (Dependabot → settings.gradle.kts).
apply(from = file("gradle/kiln.security-pins.settings.gradle.kts"))

rootProject.name = "Kiln"

include(":app")
include(":core:common")
include(":core:model")
include(":core:network")
include(":core:database")
include(":core:designsystem")
include(":core:privilege")
include(":feature:apps")
include(":feature:sources")
include(":feature:installer")
include(":feature:onboarding")
include(":feature:storage")
include(":feature:backup")
include(":feature:settings")

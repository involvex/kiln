dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

// Same CVE floors as the main build (AGP compileOnly on :convention).
apply(from = file("../gradle/kiln.security-pins.settings.gradle.kts"))

rootProject.name = "build-logic"
include(":convention")

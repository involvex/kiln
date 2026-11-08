plugins {
    `kotlin-dsl`
}

group = "dev.involvex.kiln.buildlogic"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.composeCompilerPlugin)
    compileOnly(libs.ksp.gradlePlugin)
    compileOnly(libs.hilt.android.gradlePlugin)
}

tasks {
    validatePlugins {
        enableStricterValidation.set(true)
        failOnWarning.set(true)
    }
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "kiln.android.application"
            implementationClass = "dev.involvex.kiln.convention.AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "kiln.android.library"
            implementationClass = "dev.involvex.kiln.convention.AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "kiln.android.compose"
            implementationClass = "dev.involvex.kiln.convention.AndroidComposeConventionPlugin"
        }
        register("androidHilt") {
            id = "kiln.android.hilt"
            implementationClass = "dev.involvex.kiln.convention.AndroidHiltConventionPlugin"
        }
        register("androidUnitTest") {
            id = "kiln.android.unit.test"
            implementationClass = "dev.involvex.kiln.convention.AndroidUnitTestConventionPlugin"
        }
    }
}

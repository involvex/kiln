plugins {
    alias(libs.plugins.kiln.android.application)
    alias(libs.plugins.kiln.android.compose)
    alias(libs.plugins.kiln.android.hilt)
    alias(libs.plugins.kiln.android.unit.test)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "dev.involvex.kiln"
    defaultConfig {
        applicationId = "dev.involvex.kiln"
    }
    lint {
        // local.properties is machine-local; do not fail CI/local lint on path escaping.
        disable += setOf("PropertyEscape", "AndroidGradlePluginVersion", "GradleDependency", "NewerVersionAvailable")
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(project(":core:designsystem"))
    implementation(project(":feature:apps"))
    implementation(project(":feature:sources"))
    implementation(project(":feature:storage"))
    implementation(project(":feature:backup"))
    implementation(project(":feature:settings"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.timber)
}

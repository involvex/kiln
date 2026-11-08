plugins {
    alias(libs.plugins.kiln.android.library)
    alias(libs.plugins.kiln.android.compose)
    alias(libs.plugins.kiln.android.hilt)
    alias(libs.plugins.kiln.android.unit.test)
}

android {
    namespace = "dev.involvex.kiln.feature.sources"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(project(":core:designsystem"))

    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)
}

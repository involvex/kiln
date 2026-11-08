plugins {
    alias(libs.plugins.kiln.android.library)
    alias(libs.plugins.kiln.android.compose)
    alias(libs.plugins.kiln.android.unit.test)
}

android {
    namespace = "dev.involvex.kiln.core.designsystem"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.compose.material.icons.extended)
}

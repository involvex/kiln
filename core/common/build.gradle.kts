plugins {
    alias(libs.plugins.kiln.android.library)
    alias(libs.plugins.kiln.android.hilt)
    alias(libs.plugins.kiln.android.unit.test)
}

android {
    namespace = "dev.involvex.kiln.core.common"
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.timber)
}

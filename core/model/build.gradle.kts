plugins {
    alias(libs.plugins.kiln.android.library)
    alias(libs.plugins.kiln.android.unit.test)
}

android {
    namespace = "dev.involvex.kiln.core.model"
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
}

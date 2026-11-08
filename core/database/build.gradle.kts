plugins {
    alias(libs.plugins.kiln.android.library)
    alias(libs.plugins.kiln.android.hilt)
    alias(libs.plugins.kiln.android.unit.test)
}

android {
    namespace = "dev.involvex.kiln.core.database"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:model"))

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.androidx.room.testing)
}

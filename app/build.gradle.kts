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
    // Optional CI/local upload keystore via env (see docs/RELEASE.md). Falls back to debug.
    val uploadStoreFile = providers.environmentVariable("KILN_STORE_FILE")
    val uploadStorePassword = providers.environmentVariable("KILN_STORE_PASSWORD")
    val uploadKeyAlias = providers.environmentVariable("KILN_KEY_ALIAS")
    val uploadKeyPassword = providers.environmentVariable("KILN_KEY_PASSWORD")
    val hasUploadKeystore =
        uploadStoreFile.orNull?.isNotBlank() == true &&
            uploadStorePassword.orNull?.isNotBlank() == true &&
            uploadKeyAlias.orNull?.isNotBlank() == true &&
            uploadKeyPassword.orNull?.isNotBlank() == true
    signingConfigs {
        if (hasUploadKeystore) {
            create("release") {
                storeFile = file(uploadStoreFile.get())
                storePassword = uploadStorePassword.get()
                keyAlias = uploadKeyAlias.get()
                keyPassword = uploadKeyPassword.get()
            }
        }
    }
    buildTypes {
        getByName("release") {
            signingConfig = if (hasUploadKeystore) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(project(":core:network"))
    implementation(project(":core:database"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:privilege"))
    implementation(project(":feature:apps"))
    implementation(project(":feature:sources"))
    implementation(project(":feature:installer"))
    implementation(project(":feature:onboarding"))
    implementation(project(":feature:storage"))
    implementation(project(":feature:backup"))
    implementation(project(":feature:debloat"))
    implementation(project(":feature:permissions"))
    implementation(project(":feature:termux"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:maintenance"))
    implementation(project(":feature:widgets"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.coil.compose)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.timber)
}

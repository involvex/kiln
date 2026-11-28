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
    // Omit Google Play dependency metadata from the APK/AAB signing block (IzzyOnDroid /
    // F-Droid scanners; only Google can decrypt that blob).
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
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
    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.androidx.compose.material3.adaptive.navigation3)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    implementation(libs.androidx.work.runtime)
    implementation(libs.coil.compose)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.timber)
}

// Fails when generated Hilt component sources omit a known @HiltViewModel.
// Stale KSP incremental output previously shipped APKs that crashed at runtime with
// "Cannot create an instance of …ViewModel".
tasks.register("verifyHiltViewModelMapDebug") {
    group = "verification"
    description = "Assert all listed @HiltViewModel classes appear in debug Hilt codegen"
    dependsOn("hiltJavaCompileDebug")
    val expectedFile = layout.projectDirectory.file("hilt-viewmodels.txt")
    val hiltSources = layout.buildDirectory.dir("generated/hilt/component_sources/debug")
    val repoRoot = rootProject.layout.projectDirectory
    inputs.file(expectedFile)
    inputs.dir(hiltSources)
    doLast {
        val expected = expectedFile.asFile.readLines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
        val sourcesDir = hiltSources.get().asFile
        check(sourcesDir.isDirectory) {
            "Hilt debug component sources missing at ${sourcesDir.path}. Run assembleDebug first."
        }
        val combined = sourcesDir.walkTopDown()
            .filter { it.isFile && it.extension == "java" }
            .joinToString("\n") { it.readText() }
        val missing = expected.filter { fqcn ->
            val simple = fqcn.substringAfterLast('.')
            !combined.contains(fqcn) && !combined.contains(simple)
        }
        check(missing.isEmpty()) {
            buildString {
                appendLine("Hilt ViewModel map incomplete (debug). Missing:")
                missing.forEach { appendLine("  - $it") }
                appendLine("Clean rebuild (ksp.incremental=false) and ensure each module ran KSP.")
            }
        }

        // Scan main sources only (not registered as task inputs — avoids Gradle
        // implicit-deps on feature/*/build outputs under the feature/ tree).
        val root = repoRoot.asFile
        val callPattern = Regex("""hiltViewModel\s*[<(]""")
        fun isMainKotlin(file: java.io.File): Boolean {
            val normalized = file.path.replace('\\', '/')
            return file.extension == "kt" && normalized.contains("/src/main/")
        }
        val offenders = listOf(root.resolve("feature"), root.resolve("app/src/main"))
            .filter { it.isDirectory }
            .flatMap { dir ->
                dir.walkTopDown()
                    .filter { it.isFile && isMainKotlin(it) }
                    .filter { callPattern.containsMatchIn(it.readText()) }
            }
        check(offenders.isEmpty()) {
            buildString {
                appendLine("Use kilnHiltViewModel() instead of hiltViewModel() in:")
                offenders.forEach { appendLine("  - ${it.relativeTo(root)}") }
            }
        }
    }
}

tasks.named("check") {
    dependsOn("verifyHiltViewModelMapDebug")
}

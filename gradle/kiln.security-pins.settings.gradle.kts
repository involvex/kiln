/*
 * CVE floors for Dependabot alerts on AGP / plugin classpath transitives.
 * Applied from settings.gradle.kts via apply(from = …).
 * These jars do not ship in the APK; they only affect the build toolchain.
 *
 * Keep versions in sync with gradle/libs.versions.toml
 * (guavaJre / guavaAndroid / bouncycastle / jose4j / jdom2 / commonsLang3 / httpclient / kotlin).
 */

val guavaJre = "33.8.0-jre"
val guavaAndroid = "33.8.0-android"
val bouncycastle = "1.86"
val jose4j = "0.9.6"
val jdom2 = "2.0.6.1"
val commonsLang3 = "3.18.0"
val httpclient = "4.5.14"
val kotlinGradlePlugin = "2.4.21"

fun ResolutionStrategy.applyKilnSecurityPins() {
    eachDependency {
        val group = requested.group
        val name = requested.name
        when {
            group == "com.google.guava" && name == "guava" -> {
                val target =
                    if (requested.version?.endsWith("-android") == true) {
                        guavaAndroid
                    } else {
                        guavaJre
                    }
                useVersion(target)
                because("Dependabot: Guava CompactHashMap/Set deserialization CVE floor")
            }

            group == "org.bouncycastle" && name.endsWith("-jdk18on") -> {
                useVersion(bouncycastle)
                because("Dependabot: Bouncy Castle ASN.1 / Name Constraints CVE floor")
            }

            group == "org.bitbucket.b_c" && name == "jose4j" -> {
                useVersion(jose4j)
                because("Dependabot: jose4j compressed JWE DoS CVE floor")
            }

            group == "org.jdom" && name == "jdom2" -> {
                useVersion(jdom2)
                because("Dependabot: JDOM XXE CVE floor")
            }

            group == "org.apache.commons" && name == "commons-lang3" -> {
                useVersion(commonsLang3)
                because("Dependabot: Commons Lang uncontrolled recursion CVE floor")
            }

            group == "org.apache.httpcomponents" && name == "httpclient" -> {
                useVersion(httpclient)
                because("Dependabot: Apache HttpClient XSS CVE floor")
            }

            group == "org.jetbrains.kotlin" && name == "kotlin-gradle-plugin" -> {
                useVersion(kotlinGradlePlugin)
                because("Dependabot: Kotlin build-cache deserialization CVE; match project Kotlin")
            }
        }
    }
}

gradle.beforeProject {
    buildscript.configurations.configureEach {
        resolutionStrategy.applyKilnSecurityPins()
    }
    configurations.configureEach {
        resolutionStrategy.applyKilnSecurityPins()
    }
}

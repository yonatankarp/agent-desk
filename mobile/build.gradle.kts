import org.gradle.api.tasks.testing.Test

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kover)
}

kotlin {
    jvmToolchain(25)

    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":app"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
        }
        jvmTest.dependencies {
            implementation(libs.konsist)
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    failOnNoDiscoveredTests = true
}

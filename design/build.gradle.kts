import org.gradle.api.tasks.testing.Test
import org.jetbrains.compose.ExperimentalComposeLibrary

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
            implementation(compose.components.resources)
        }
        commonTest.dependencies {
            implementation(libs.kotest.assertions.core)
            implementation(libs.kotest.framework.engine)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
        }
        jvmTest.dependencies {
            // Tracked debt #279: no stable Compose UI-test API yet. Test-only opt-in.
            @OptIn(ExperimentalComposeLibrary::class)
            implementation(compose.uiTest)
            implementation(project(":core"))
            implementation(project(":test-fixtures"))
            implementation(libs.konsist)
            implementation(libs.kotest.runner.junit5)
        }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "com.yonatankarp.agentdesk.design.resources"
    generateResClass = always
}

kover {
    reports {
        verify {
            rule("minimum line coverage") {
                minBound(90)
            }
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    failOnNoDiscoveredTests = true
}

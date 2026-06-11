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
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.components.resources)
        }
        commonTest.dependencies {
            implementation(libs.kotest.assertions.core)
            implementation(libs.kotest.framework.engine)
        }
        jvmMain.dependencies {
            implementation(currentComposeDesktopDependency())
        }
        jvmTest.dependencies {
            // Tracked debt #279: no stable Compose UI-test API yet. Test-only opt-in.
            implementation(libs.compose.ui.test)
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

fun currentComposeDesktopDependency() = when (val os = System.getProperty("os.name").lowercase()) {
    in listOf("mac os x", "darwin") -> {
        when (System.getProperty("os.arch").lowercase()) {
            "aarch64", "arm64" -> libs.compose.desktop.macos.arm64
            else -> libs.compose.desktop.macos.x64
        }
    }

    else -> when {
        os.contains("windows") -> libs.compose.desktop.windows.x64

        os.contains("linux") && System.getProperty("os.arch").lowercase() in listOf("aarch64", "arm64") ->
            libs.compose.desktop.linux.arm64

        else -> libs.compose.desktop.linux.x64
    }
}

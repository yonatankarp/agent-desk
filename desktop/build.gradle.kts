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
            implementation(project(":core"))
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
            @OptIn(ExperimentalComposeLibrary::class)
            implementation(compose.uiTest)
            implementation(libs.konsist)
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.yonatankarp.agentdesk.desktop.AgentDeskDesktopKt"
        // Launch with the toolchain JDK instead of the Gradle daemon JVM,
        // which may be older than the class files produced by the toolchain.
        javaHome = javaToolchains.launcherFor {
            languageVersion = JavaLanguageVersion.of(25)
        }.get().metadata.installationPath.asFile.absolutePath
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    failOnNoDiscoveredTests = true
}

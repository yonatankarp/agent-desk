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
            implementation(project(":core"))
            implementation(project(":design"))
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
        }
        commonTest.dependencies {
            implementation(libs.kotest.assertions.core)
            implementation(libs.kotest.framework.engine)
            implementation(project(":test-fixtures"))
        }
        jvmMain.dependencies {
            implementation(currentComposeDesktopDependency())
        }
        jvmTest.dependencies {
            implementation(libs.compose.ui.test)
            implementation(libs.konsist)
            implementation(libs.kotest.runner.junit5)
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

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotest)
    alias(libs.plugins.kover)
}

kotlin {
    jvmToolchain(25)

    jvm()

    sourceSets {
        commonTest.dependencies {
            implementation(libs.kotest.assertions.core)
            implementation(libs.kotest.framework.engine)
        }
        jvmTest.dependencies {
            implementation(libs.konsist)
        }
    }
}

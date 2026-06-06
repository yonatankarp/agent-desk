import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.tasks.JavaExec

plugins {
    application
    alias(libs.plugins.kover)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.shadow)
}

kotlin {
    jvmToolchain(25)
}

application {
    mainClass = "com.yonatankarp.agentdesk.cli.AgentDeskCliKt"
}

dependencies {
    implementation(project(":app"))
    implementation(project(":core"))
    testImplementation(project(":test-fixtures"))
    testImplementation(libs.konsist)
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
}

kover {
    reports {
        verify {
            rule("minimum line coverage") {
                minBound(88)
            }
        }
    }
}

tasks.test {
    useJUnitPlatform()
    failOnNoDiscoveredTests = true
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}

val executableJar by tasks.registering(ShadowJar::class) {
    group = "distribution"
    description = "Assembles a standalone executable CLI jar."

    archiveFileName = "agent-desk-cli-all.jar"
    mergeServiceFiles()

    manifest {
        attributes["Main-Class"] = application.mainClass.get()
    }

    exclude("META-INF/*.DSA", "META-INF/*.RSA", "META-INF/*.SF")
    from(sourceSets.main.get().output)
    configurations = listOf(project.configurations.runtimeClasspath.get())
}

tasks.named("assemble") {
    dependsOn(executableJar)
}

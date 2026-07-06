import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.JavaExec

plugins {
    application
    alias(libs.plugins.kover)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.shadow) apply false
}

kotlin {
    jvmToolchain(25)
}

apply(plugin = "com.gradleup.shadow")

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

tasks.withType<ShadowJar>().configureEach {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    exclude("META-INF/*.DSA", "META-INF/*.RSA", "META-INF/*.SF", "META-INF/versions/**/module-info.class")
}

val executableJar by tasks.registering(ShadowJar::class) {
    group = "distribution"
    description = "Assembles a standalone executable CLI jar."

    archiveFileName = "agent-desk-cli-all.jar"
    mergeServiceFiles()

    manifest {
        attributes["Main-Class"] = application.mainClass.get()
    }

    from(sourceSets.main.get().output)
    configurations = listOf(project.configurations.runtimeClasspath.get())
}

tasks.named("assemble") {
    dependsOn(executableJar)
}

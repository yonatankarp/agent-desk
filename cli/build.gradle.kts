import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.JavaExec
import org.gradle.jvm.tasks.Jar

plugins {
    application
    alias(libs.plugins.kotlin.jvm)
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
    testImplementation(libs.konsist)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}

val executableJar by tasks.registering(Jar::class) {
    group = "distribution"
    description = "Assembles a standalone executable CLI jar."

    archiveBaseName = "agent-desk-cli"
    archiveClassifier = "all"
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    manifest {
        attributes["Main-Class"] = application.mainClass.get()
    }

    exclude("META-INF/*.DSA", "META-INF/*.RSA", "META-INF/*.SF")

    from(sourceSets.main.get().output)
    from({
        configurations.runtimeClasspath.get()
            .filter { it.exists() }
            .map { if (it.isDirectory) it else zipTree(it) }
    })
}

tasks.named("assemble") {
    dependsOn(executableJar)
}

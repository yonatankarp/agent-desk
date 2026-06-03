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

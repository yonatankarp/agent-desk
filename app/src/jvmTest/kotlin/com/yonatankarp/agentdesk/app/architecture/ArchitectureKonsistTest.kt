package com.yonatankarp.agentdesk.app.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue

class ArchitectureKonsistTest :
    FunSpec({
        test("app production declarations stay under the shared app package") {
            Konsist
                .scopeFromProject(moduleName = "app", sourceSetName = "commonMain")
                .classes()
                .assertTrue {
                    it.resideInPackage("com.yonatankarp.agentdesk.app..")
                }
        }

        test("app JVM production declarations stay under the shared app package") {
            Konsist
                .scopeFromProject(moduleName = "app", sourceSetName = "jvmMain")
                .classes()
                .assertTrue {
                    it.resideInPackage("com.yonatankarp.agentdesk.app..")
                }
        }

        test("app production files do not import adapter or private runtime packages") {
            Konsist
                .scopeFromProject(moduleName = "app", sourceSetName = "commonMain")
                .files
                .assertTrue { file ->
                    !file.hasBlockedImport()
                }
        }

        test("app JVM production files do not import client or private runtime packages") {
            Konsist
                .scopeFromProject(moduleName = "app", sourceSetName = "jvmMain")
                .files
                .assertTrue { file ->
                    !file.hasBlockedImport()
                }
        }

        test("app test files use Kotest instead of kotlin.test or JUnit") {
            appTestSourceSets.forEach { sourceSet ->
                Konsist
                    .scopeFromProject(moduleName = "app", sourceSetName = sourceSet)
                    .files
                    .assertTrue { file ->
                        !file.hasImport { import ->
                            blockedTestFrameworkPrefixes.any { import.name.startsWith(it) }
                        }
                    }
            }
        }

        test("forbidden app JVM import guard catches client fixtures") {
            val fixture =
                Konsist
                    .scopeFromFile("app/src/jvmTest/resources/architecture/ForbiddenAppJvmImportFixture.kt")
                    .files
                    .single()

            fixture.hasBlockedImport().shouldBeTrue()
        }
    }) {
    companion object {
        private val appTestSourceSets = listOf("commonTest", "jvmTest")

        // Kotest is the only test framework (docs/engineering-style.md).
        private val blockedTestFrameworkPrefixes =
            listOf(
                "kotlin.test.",
                "org.junit.",
            )

        private val blockedImportPrefixes =
            listOf(
                "com.yonatankarp.agentdesk.adapter.",
                "com.yonatankarp.agentdesk.cli.",
                "com.yonatankarp.agentdesk.desktop.",
                "com.yonatankarp.agentdesk.openclaw.",
                "com.yonatankarp.agentdesk.runtime.",
                "com.yonatankarp.agentdesk.ui.",
            )

        private fun KoFileDeclaration.hasBlockedImport(): Boolean = hasImport { import ->
            blockedImportPrefixes.any { import.name.startsWith(it) }
        }
    }
}

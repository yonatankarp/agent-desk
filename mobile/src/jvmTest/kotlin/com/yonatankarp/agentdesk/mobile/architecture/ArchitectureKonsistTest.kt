package com.yonatankarp.agentdesk.mobile.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ArchitectureKonsistTest :
    FunSpec({
        test("mobile production declarations stay under the mobile package") {
            mobileProductionSourceSets.forEach { sourceSet ->
                Konsist
                    .scopeFromProject(moduleName = "mobile", sourceSetName = sourceSet)
                    .classes()
                    .assertTrue {
                        it.resideInPackage("com.yonatankarp.agentdesk.mobile..")
                    }
            }
        }

        test("mobile production files do not import other clients or private runtime packages") {
            mobileProductionSourceSets.forEach { sourceSet ->
                Konsist
                    .scopeFromProject(moduleName = "mobile", sourceSetName = sourceSet)
                    .files
                    .assertTrue { file ->
                        !file.hasBlockedImport()
                    }
            }
        }

        test("mobile test files use Kotest instead of kotlin.test or JUnit") {
            mobileTestSourceSets.forEach { sourceSet ->
                Konsist
                    .scopeFromProject(moduleName = "mobile", sourceSetName = sourceSet)
                    .files
                    .assertTrue { file ->
                        !file.hasImport { import ->
                            blockedTestFrameworkPrefixes.any { import.name.startsWith(it) }
                        }
                    }
            }
        }

        test("forbidden mobile JVM import guard catches client fixtures") {
            val fixture =
                Konsist
                    .scopeFromFile("mobile/src/jvmTest/resources/architecture/ForbiddenMobileJvmImportFixture.kt")
                    .files
                    .single()

            fixture.hasBlockedImport() shouldBe true
        }
    }) {
    private companion object {
        private val mobileProductionSourceSets = listOf("commonMain", "jvmMain")

        private val mobileTestSourceSets = listOf("commonTest", "jvmTest")

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

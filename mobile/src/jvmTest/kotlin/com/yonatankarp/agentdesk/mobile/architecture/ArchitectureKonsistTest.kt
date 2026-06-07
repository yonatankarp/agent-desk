package com.yonatankarp.agentdesk.mobile.architecture

import com.lemonappdev.konsist.api.Konsist
import com.yonatankarp.agentdesk.testfixtures.architecture.ModuleArchitectureRules
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private val blockedImportPrefixes =
    listOf(
        "com.yonatankarp.agentdesk.adapter.",
        "com.yonatankarp.agentdesk.cli.",
        "com.yonatankarp.agentdesk.desktop.",
        "com.yonatankarp.agentdesk.openclaw.",
        "com.yonatankarp.agentdesk.runtime.",
        "com.yonatankarp.agentdesk.app.runtime.OpenClaw",
        "com.yonatankarp.agentdesk.ui.",
    )

class ArchitectureKonsistTest :
    FunSpec({
        test("mobile production declarations stay under the mobile package") {
            ModuleArchitectureRules.assertProductionDeclarationsResideIn(
                moduleName = "mobile",
                sourceSets = listOf("commonMain", "jvmMain"),
                packagePattern = "com.yonatankarp.agentdesk.mobile..",
            )
        }

        test("mobile production files do not import other clients or private runtime packages") {
            ModuleArchitectureRules.assertNoBlockedImports(
                moduleName = "mobile",
                sourceSets = listOf("commonMain", "jvmMain"),
                blockedImportPrefixes = blockedImportPrefixes,
            )
        }

        test("mobile test files use Kotest instead of kotlin.test or JUnit") {
            ModuleArchitectureRules.assertKotestOnly(
                moduleName = "mobile",
                testSourceSets = listOf("commonTest", "jvmTest"),
            )
        }

        test("forbidden mobile JVM import guard catches client fixtures") {
            val fixture =
                Konsist
                    .scopeFromFile("mobile/src/jvmTest/resources/architecture/ForbiddenMobileJvmImportFixture.kt")
                    .files
                    .single()

            ModuleArchitectureRules.hasBlockedImport(fixture, blockedImportPrefixes) shouldBe true
        }
    })

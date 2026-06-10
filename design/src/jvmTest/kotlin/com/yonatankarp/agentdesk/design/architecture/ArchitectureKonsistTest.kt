package com.yonatankarp.agentdesk.design.architecture

import com.lemonappdev.konsist.api.Konsist
import com.yonatankarp.agentdesk.testfixtures.architecture.ModuleArchitectureRules
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue

private val blockedImportPrefixes =
    listOf(
        "com.yonatankarp.agentdesk.adapter.",
        "com.yonatankarp.agentdesk.cli.",
        "com.yonatankarp.agentdesk.desktop.",
        "com.yonatankarp.agentdesk.mobile.",
        "com.yonatankarp.agentdesk.openclaw.",
        "com.yonatankarp.agentdesk.runtime.",
    )

class ArchitectureKonsistTest :
    FunSpec({
        test("design production declarations stay under the design package") {
            ModuleArchitectureRules.assertProductionDeclarationsResideIn(
                moduleName = "design",
                sourceSets = listOf("commonMain"),
                packagePattern = "com.yonatankarp.agentdesk.design..",
            )
        }

        test("design production files do not import client or runtime packages") {
            ModuleArchitectureRules.assertNoBlockedImports(
                moduleName = "design",
                sourceSets = listOf("commonMain"),
                blockedImportPrefixes = blockedImportPrefixes,
            )
        }

        test("design test files use Kotest instead of kotlin.test or JUnit") {
            ModuleArchitectureRules.assertKotestOnly(
                moduleName = "design",
                testSourceSets = listOf("commonTest", "jvmTest"),
            )
        }

        test("forbidden design import guard catches client imports") {
            val fixture =
                Konsist
                    .scopeFromFile("design/src/jvmTest/resources/architecture/ForbiddenDesignImportFixture.kt")
                    .files
                    .single()

            ModuleArchitectureRules.hasBlockedImport(fixture, blockedImportPrefixes).shouldBeTrue()
        }
    })

package com.yonatankarp.agentdesk.app.architecture

import com.lemonappdev.konsist.api.Konsist
import com.yonatankarp.agentdesk.testfixtures.architecture.ModuleArchitectureRules
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue

private val blockedImportPrefixes =
    listOf(
        "com.yonatankarp.agentdesk.adapter.",
        "com.yonatankarp.agentdesk.cli.",
        "com.yonatankarp.agentdesk.desktop.",
        "com.yonatankarp.agentdesk.openclaw.",
        "com.yonatankarp.agentdesk.runtime.",
        "com.yonatankarp.agentdesk.ui.",
    )

class ArchitectureKonsistTest :
    FunSpec({
        test("app production declarations stay under the shared app package") {
            ModuleArchitectureRules.assertProductionDeclarationsResideIn(
                moduleName = "app",
                sourceSets = listOf("commonMain"),
                packagePattern = "com.yonatankarp.agentdesk.app..",
            )
        }

        test("app JVM production declarations stay under the shared app package") {
            ModuleArchitectureRules.assertProductionDeclarationsResideIn(
                moduleName = "app",
                sourceSets = listOf("jvmMain"),
                packagePattern = "com.yonatankarp.agentdesk.app..",
            )
        }

        test("app production files do not import adapter or private runtime packages") {
            ModuleArchitectureRules.assertNoBlockedImports(
                moduleName = "app",
                sourceSets = listOf("commonMain"),
                blockedImportPrefixes = blockedImportPrefixes,
            )
        }

        test("app JVM production files do not import client or private runtime packages") {
            ModuleArchitectureRules.assertNoBlockedImports(
                moduleName = "app",
                sourceSets = listOf("jvmMain"),
                blockedImportPrefixes = blockedImportPrefixes,
            )
        }

        test("app test files use Kotest instead of kotlin.test or JUnit") {
            ModuleArchitectureRules.assertKotestOnly(
                moduleName = "app",
                testSourceSets = listOf("commonTest", "jvmTest"),
            )
        }

        test("forbidden app JVM import guard catches client fixtures") {
            val fixture =
                Konsist
                    .scopeFromFile("app/src/jvmTest/resources/architecture/ForbiddenAppJvmImportFixture.kt")
                    .files
                    .single()

            ModuleArchitectureRules.hasBlockedImport(fixture, blockedImportPrefixes).shouldBeTrue()
        }
    })

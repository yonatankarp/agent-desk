package com.yonatankarp.agentdesk.desktop.architecture

import com.lemonappdev.konsist.api.Konsist
import com.yonatankarp.agentdesk.testfixtures.architecture.ModuleArchitectureRules
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue

private val blockedImportPrefixes =
    listOf(
        "com.yonatankarp.agentdesk.adapter.",
        "com.yonatankarp.agentdesk.cli.",
        "com.yonatankarp.agentdesk.mobile.",
        "com.yonatankarp.agentdesk.openclaw.",
        "com.yonatankarp.agentdesk.runtime.",
        "com.yonatankarp.agentdesk.app.runtime.OpenClaw",
        "com.yonatankarp.agentdesk.ui.",
    )

class ArchitectureKonsistTest :
    FunSpec({
        test("desktop production declarations stay under the desktop package") {
            ModuleArchitectureRules.assertProductionDeclarationsResideIn(
                moduleName = "desktop",
                sourceSets = listOf("commonMain", "jvmMain"),
                packagePattern = "com.yonatankarp.agentdesk.desktop..",
            )
        }

        test("desktop production files do not import other clients or private runtime packages") {
            ModuleArchitectureRules.assertNoBlockedImports(
                moduleName = "desktop",
                sourceSets = listOf("commonMain", "jvmMain"),
                blockedImportPrefixes = blockedImportPrefixes,
            )
        }

        test("every desktop blocked-import prefix is load-bearing") {
            ModuleArchitectureRules.assertEveryBlockedPrefixIsLoadBearing(blockedImportPrefixes)
        }

        test("desktop test files use Kotest instead of kotlin.test or JUnit") {
            ModuleArchitectureRules.assertKotestOnly(
                moduleName = "desktop",
                testSourceSets = listOf("commonTest", "jvmTest"),
            )
        }

        test("forbidden desktop JVM import guard catches concrete runtime adapter fixtures") {
            val fixture =
                Konsist
                    .scopeFromFile("desktop/src/jvmTest/resources/architecture/ForbiddenDesktopJvmImportFixture.kt")
                    .files
                    .single()

            ModuleArchitectureRules.hasBlockedImport(fixture, blockedImportPrefixes).shouldBeTrue()
        }
    })

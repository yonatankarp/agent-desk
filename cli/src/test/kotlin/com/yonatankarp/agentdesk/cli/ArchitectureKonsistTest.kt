package com.yonatankarp.agentdesk.cli

import com.yonatankarp.agentdesk.testfixtures.architecture.ModuleArchitectureRules
import io.kotest.core.spec.style.FunSpec

private val blockedImportPrefixes =
    listOf(
        "com.yonatankarp.agentdesk.adapter.",
        "com.yonatankarp.agentdesk.openclaw.",
        "com.yonatankarp.agentdesk.runtime.",
        "com.yonatankarp.agentdesk.desktop.",
        "com.yonatankarp.agentdesk.ui.",
    )

class ArchitectureKonsistTest :
    FunSpec({
        test("cli production declarations stay under the cli package") {
            ModuleArchitectureRules.assertProductionDeclarationsResideIn(
                moduleName = "cli",
                sourceSets = listOf("main"),
                packagePattern = "com.yonatankarp.agentdesk.cli..",
            )
        }

        test("cli production files do not import private runtime or adapter packages") {
            ModuleArchitectureRules.assertNoBlockedImports(
                moduleName = "cli",
                sourceSets = listOf("main"),
                blockedImportPrefixes = blockedImportPrefixes,
            )
        }

        test("cli test files use Kotest instead of kotlin.test or JUnit") {
            ModuleArchitectureRules.assertKotestOnly(
                moduleName = "cli",
                testSourceSets = listOf("test"),
            )
        }
    })

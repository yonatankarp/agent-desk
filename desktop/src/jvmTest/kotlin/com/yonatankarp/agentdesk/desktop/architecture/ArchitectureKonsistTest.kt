package com.yonatankarp.agentdesk.desktop.architecture

import com.yonatankarp.agentdesk.testfixtures.architecture.ModuleArchitectureRules
import io.kotest.core.spec.style.FunSpec

private val blockedImportPrefixes =
    listOf(
        "com.yonatankarp.agentdesk.adapter.",
        "com.yonatankarp.agentdesk.cli.",
        "com.yonatankarp.agentdesk.mobile.",
        "com.yonatankarp.agentdesk.openclaw.",
        "com.yonatankarp.agentdesk.runtime.",
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

        test("desktop test files use Kotest instead of kotlin.test or JUnit") {
            ModuleArchitectureRules.assertKotestOnly(
                moduleName = "desktop",
                testSourceSets = listOf("commonTest", "jvmTest"),
            )
        }
    })

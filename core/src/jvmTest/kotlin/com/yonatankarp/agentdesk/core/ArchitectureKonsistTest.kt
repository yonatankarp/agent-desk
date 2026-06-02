package com.yonatankarp.agentdesk.core

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.FunSpec

class ArchitectureKonsistTest :
    FunSpec({
        test("production declarations stay under the public Agent Desk namespace") {
            Konsist
                .scopeFromProduction()
                .classes()
                .assertTrue {
                    it.resideInPackage("com.yonatankarp.agentdesk..")
                }
        }

        test("core declarations stay in the core package") {
            Konsist
                .scopeFromProject(moduleName = "core", sourceSetName = "commonMain")
                .classes()
                .assertTrue {
                    it.resideInPackage("com.yonatankarp.agentdesk.core..")
                }
        }

        test("core production files do not import adapter or UI packages") {
            Konsist
                .scopeFromProject(moduleName = "core", sourceSetName = "commonMain")
                .files
                .assertTrue { file ->
                    !file.hasImport { import ->
                        blockedImportPrefixes.any { import.name.startsWith(it) }
                    }
                }
        }
    }) {
    companion object {
        private val blockedImportPrefixes =
            listOf(
                "com.yonatankarp.agentdesk.adapter.",
                "com.yonatankarp.agentdesk.desktop.",
                "com.yonatankarp.agentdesk.ui.",
            )
    }
}

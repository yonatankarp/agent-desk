package com.yonatankarp.agentdesk.cli

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.FunSpec

class ArchitectureKonsistTest :
    FunSpec({
        test("cli production declarations stay under the cli package") {
            Konsist
                .scopeFromProject(moduleName = "cli", sourceSetName = "main")
                .classes()
                .assertTrue {
                    it.resideInPackage("com.yonatankarp.agentdesk.cli..")
                }
        }

        test("cli production files do not import private runtime or adapter packages") {
            Konsist
                .scopeFromProject(moduleName = "cli", sourceSetName = "main")
                .files
                .assertTrue { file ->
                    !file.hasImport { import ->
                        blockedImportPrefixes.any { import.name.startsWith(it) }
                    }
                }
        }

        test("cli test files use Kotest instead of kotlin.test or JUnit") {
            Konsist
                .scopeFromProject(moduleName = "cli", sourceSetName = "test")
                .files
                .assertTrue { file ->
                    !file.hasImport { import ->
                        blockedTestFrameworkPrefixes.any { import.name.startsWith(it) }
                    }
                }
        }
    }) {
    companion object {
        private val blockedImportPrefixes =
            listOf(
                "com.yonatankarp.agentdesk.adapter.",
                "com.yonatankarp.agentdesk.openclaw.",
                "com.yonatankarp.agentdesk.runtime.",
                "com.yonatankarp.agentdesk.desktop.",
                "com.yonatankarp.agentdesk.ui.",
            )

        // Kotest is the only test framework (docs/engineering-style.md).
        private val blockedTestFrameworkPrefixes =
            listOf(
                "kotlin.test.",
                "org.junit.",
            )
    }
}

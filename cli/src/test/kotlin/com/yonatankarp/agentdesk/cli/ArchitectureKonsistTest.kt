package com.yonatankarp.agentdesk.cli

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertTrue
import kotlin.test.Test

class ArchitectureKonsistTest {
    @Test
    fun `cli production declarations stay under the cli package`() {
        Konsist
            .scopeFromProject(moduleName = "cli", sourceSetName = "main")
            .classes()
            .assertTrue {
                it.resideInPackage("com.yonatankarp.agentdesk.cli..")
            }
    }

    @Test
    fun `cli production files do not import private runtime or adapter packages`() {
        Konsist
            .scopeFromProject(moduleName = "cli", sourceSetName = "main")
            .files
            .assertTrue { file ->
                !file.hasImport { import ->
                    blockedImportPrefixes.any { import.name.startsWith(it) }
                }
            }
    }

    companion object {
        private val blockedImportPrefixes =
            listOf(
                "com.yonatankarp.agentdesk.adapter.",
                "com.yonatankarp.agentdesk.openclaw.",
                "com.yonatankarp.agentdesk.runtime.",
                "com.yonatankarp.agentdesk.desktop.",
                "com.yonatankarp.agentdesk.ui.",
            )
    }
}

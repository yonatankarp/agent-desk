package com.yonatankarp.agentdesk.app.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.FunSpec

class ArchitectureKonsistTest :
    FunSpec({
        test("app production declarations stay under the shared app package") {
            Konsist
                .scopeFromProject(moduleName = "app", sourceSetName = "commonMain")
                .classes()
                .assertTrue {
                    it.resideInPackage("com.yonatankarp.agentdesk.app..")
                }
        }

        test("app production files do not import adapter or private runtime packages") {
            Konsist
                .scopeFromProject(moduleName = "app", sourceSetName = "commonMain")
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
                "com.yonatankarp.agentdesk.cli.",
                "com.yonatankarp.agentdesk.desktop.",
                "com.yonatankarp.agentdesk.openclaw.",
                "com.yonatankarp.agentdesk.runtime.",
                "com.yonatankarp.agentdesk.ui.",
            )
    }
}

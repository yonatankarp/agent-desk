package com.yonatankarp.agentdesk.desktop.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.FunSpec

private val desktopProductionSourceSets = listOf("commonMain", "jvmMain")

private val blockedImportPrefixes =
    listOf(
        "com.yonatankarp.agentdesk.adapter.",
        "com.yonatankarp.agentdesk.cli.",
        "com.yonatankarp.agentdesk.mobile.",
        "com.yonatankarp.agentdesk.openclaw.",
        "com.yonatankarp.agentdesk.runtime.",
        "com.yonatankarp.agentdesk.ui.",
    )

private fun KoFileDeclaration.hasBlockedImport(): Boolean = hasImport { import ->
    blockedImportPrefixes.any { import.name.startsWith(it) }
}

class ArchitectureKonsistTest :
    FunSpec({
        test("desktop production declarations stay under the desktop package") {
            desktopProductionSourceSets.forEach { sourceSet ->
                Konsist
                    .scopeFromProject(moduleName = "desktop", sourceSetName = sourceSet)
                    .classes()
                    .assertTrue {
                        it.resideInPackage("com.yonatankarp.agentdesk.desktop..")
                    }
            }
        }

        test("desktop production files do not import other clients or private runtime packages") {
            desktopProductionSourceSets.forEach { sourceSet ->
                Konsist
                    .scopeFromProject(moduleName = "desktop", sourceSetName = sourceSet)
                    .files
                    .assertTrue { file ->
                        !file.hasBlockedImport()
                    }
            }
        }
    })

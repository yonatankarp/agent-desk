package com.yonatankarp.agentdesk.core.architecture

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

        test("core declarations stay under the domain package") {
            Konsist
                .scopeFromProject(moduleName = "core", sourceSetName = "commonMain")
                .classes()
                .assertTrue {
                    it.resideInPackage("com.yonatankarp.agentdesk.core.domain..")
                }
        }

        test("core entity declarations stay in the entities package") {
            Konsist
                .scopeFromProject(moduleName = "core", sourceSetName = "commonMain")
                .classes()
                .filter { it.name == "WorkItem" }
                .assertTrue {
                    it.resideInPackage("com.yonatankarp.agentdesk.core.domain.entities..")
                }
        }

        test("core event declarations stay in the events package") {
            Konsist
                .scopeFromProject(moduleName = "core", sourceSetName = "commonMain")
                .classes()
                .filter { it.name in eventDeclarationNames }
                .assertTrue {
                    it.resideInPackage("com.yonatankarp.agentdesk.core.domain.events..")
                }
        }

        test("core value object declarations stay in the valueobjects package") {
            Konsist
                .scopeFromProject(moduleName = "core", sourceSetName = "commonMain")
                .classes()
                .filter {
                    it.name in
                        setOf(
                            "WorkItemId",
                            "WorkItemTitle",
                            "WorkStatus",
                            "WorkSummary",
                        )
                }
                .assertTrue {
                    it.resideInPackage("com.yonatankarp.agentdesk.core.domain.valueobjects..")
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
        private val eventDeclarationNames =
            setOf(
                "EventSource",
                "EventTimestamp",
                "WorkBlockedPayload",
                "WorkCanceledPayload",
                "WorkEvent",
                "WorkEventId",
                "WorkEventPayload",
                "WorkEventType",
                "WorkFailedPayload",
                "WorkNeedsDecisionPayload",
                "WorkStartedPayload",
                "WorkSucceededPayload",
            )

        private val blockedImportPrefixes =
            listOf(
                "com.yonatankarp.agentdesk.adapter.",
                "com.yonatankarp.agentdesk.cli.",
                "com.yonatankarp.agentdesk.desktop.",
                "com.yonatankarp.agentdesk.ui.",
            )
    }
}

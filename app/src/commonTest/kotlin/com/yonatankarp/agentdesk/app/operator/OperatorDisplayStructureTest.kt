package com.yonatankarp.agentdesk.app.operator

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class OperatorDisplayStructureTest :
    FunSpec({
        test("canonical display sections keep the documented order") {
            OperatorDisplayStructure.orderedSections.map { section -> section.canonicalTitle } shouldBe listOf(
                "Replay status",
                "Work state",
                "Timeline",
                "Decision queue",
                "Evidence detail",
            )
        }

        test("desktop and mobile labels are explicitly mapped to canonical sections") {
            OperatorDisplayStructure.orderedSections.map { section ->
                section.canonicalTitle to (section.desktopLabel to section.mobileLabel)
            } shouldBe listOf(
                "Replay status" to ("Replay status" to "Projection warnings"),
                "Work state" to ("Work state" to "Current work"),
                "Timeline" to ("Read-only timeline" to "Timeline"),
                "Decision queue" to ("Decision queue" to "Attention queue"),
                "Evidence detail" to ("Evidence drilldown" to "Evidence detail"),
            )
        }

        test("settings is a separate app page, not part of the main operator section order") {
            OperatorDisplayStructure.settingsSection.canonicalTitle shouldBe "Settings"
            OperatorDisplayStructure.settingsSection.desktopLabel shouldBe "Settings"
            OperatorDisplayStructure.settingsSection.mobileLabel shouldBe "Settings"
            OperatorDisplayStructure.orderedSections.contains(OperatorDisplayStructure.settingsSection) shouldBe false
        }
    })

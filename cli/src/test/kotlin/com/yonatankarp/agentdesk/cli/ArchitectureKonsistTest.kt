package com.yonatankarp.agentdesk.cli

import com.lemonappdev.konsist.api.Konsist
import com.yonatankarp.agentdesk.testfixtures.architecture.ModuleArchitectureRules
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue

private val blockedImportPrefixes =
    listOf(
        "com.yonatankarp.agentdesk.adapter.",
        "com.yonatankarp.agentdesk.openclaw.",
        "com.yonatankarp.agentdesk.runtime.",
        "com.yonatankarp.agentdesk.desktop.",
        "com.yonatankarp.agentdesk.ui.",
    )

private val inputLayerBlockedImportPrefixes =
    listOf(
        "com.yonatankarp.agentdesk.cli.ActCommand",
        "com.yonatankarp.agentdesk.cli.AgentDeskCli",
        "com.yonatankarp.agentdesk.cli.main",
        "com.yonatankarp.agentdesk.cli.io.",
        "com.yonatankarp.agentdesk.cli.render.",
    )

private val renderLayerBlockedImportPrefixes =
    listOf(
        "com.yonatankarp.agentdesk.cli.input.",
        "com.yonatankarp.agentdesk.cli.io.",
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

        test("cli input production files do not import root, io, or render cli layers") {
            ModuleArchitectureRules.assertNoBlockedImportsInPackages(
                moduleName = "cli",
                sourceSets = listOf("main"),
                packageNames = listOf("com.yonatankarp.agentdesk.cli.input"),
                blockedImportPrefixes = inputLayerBlockedImportPrefixes,
            )
        }

        test("cli render production files do not import sibling cli layers") {
            ModuleArchitectureRules.assertNoBlockedImportsInPackages(
                moduleName = "cli",
                sourceSets = listOf("main"),
                packageNames = listOf("com.yonatankarp.agentdesk.cli.render"),
                blockedImportPrefixes = renderLayerBlockedImportPrefixes,
            )
        }

        test("cli test files use Kotest instead of kotlin.test or JUnit") {
            ModuleArchitectureRules.assertKotestOnly(
                moduleName = "cli",
                testSourceSets = listOf("test"),
            )
        }

        test("forbidden cli input layer import guard catches sibling and root fixtures") {
            val fixture =
                Konsist
                    .scopeFromFile("cli/src/test/resources/architecture/ForbiddenCliInputLayerImportFixture.kt")
                    .files
                    .single()

            fixture.hasPackage("com.yonatankarp.agentdesk.cli.input.fixture").shouldBeTrue()
            ModuleArchitectureRules.hasBlockedImport(fixture, inputLayerBlockedImportPrefixes).shouldBeTrue()
        }
    })

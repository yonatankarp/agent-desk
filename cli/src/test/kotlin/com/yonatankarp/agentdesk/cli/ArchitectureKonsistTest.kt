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

private const val CLI_ROOT_PACKAGE = "com.yonatankarp.agentdesk.cli"

// The #267 layering: root -> {input, io, render}; io -> input stays allowed.
private val inputBlockedSiblingPrefixes =
    listOf(
        "$CLI_ROOT_PACKAGE.io.",
        "$CLI_ROOT_PACKAGE.render.",
    )

private val renderBlockedSiblingPrefixes =
    listOf(
        "$CLI_ROOT_PACKAGE.input.",
        "$CLI_ROOT_PACKAGE.io.",
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

        test("cli.input does not import cli.io or cli.render types") {
            ModuleArchitectureRules.assertSubPackageDoesNotImport(
                moduleName = "cli",
                sourceSets = listOf("main"),
                packageName = "$CLI_ROOT_PACKAGE.input",
                blockedImportPrefixes = inputBlockedSiblingPrefixes,
            )
        }

        test("cli.input does not import root cli types") {
            ModuleArchitectureRules.assertSubPackageDoesNotImportExactPackage(
                moduleName = "cli",
                sourceSets = listOf("main"),
                packageName = "$CLI_ROOT_PACKAGE.input",
                forbiddenExactPackage = CLI_ROOT_PACKAGE,
            )
        }

        test("cli.render does not import sibling cli sub-package types") {
            ModuleArchitectureRules.assertSubPackageDoesNotImport(
                moduleName = "cli",
                sourceSets = listOf("main"),
                packageName = "$CLI_ROOT_PACKAGE.render",
                blockedImportPrefixes = renderBlockedSiblingPrefixes,
            )
        }

        test("forbidden cli layering guard catches sibling and root imports") {
            val fixture =
                Konsist
                    .scopeFromFile("cli/src/test/resources/architecture/ForbiddenCliLayeringFixture.kt")
                    .files
                    .single()

            ModuleArchitectureRules.hasBlockedImport(fixture, inputBlockedSiblingPrefixes).shouldBeTrue()
            ModuleArchitectureRules.hasBlockedImport(fixture, renderBlockedSiblingPrefixes).shouldBeTrue()
            ModuleArchitectureRules.hasImportFromExactPackage(fixture, CLI_ROOT_PACKAGE).shouldBeTrue()
        }
    })

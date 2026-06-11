package com.yonatankarp.agentdesk.testfixtures.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.assertions.withClue
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldNotContainDuplicates

object ModuleArchitectureRules {
    // Kotest is the only test framework (docs/engineering-style.md).
    private val blockedTestFrameworkPrefixes =
        listOf(
            "kotlin.test.",
            "org.junit.",
        )

    fun assertProductionDeclarationsResideIn(
        moduleName: String,
        sourceSets: List<String>,
        packagePattern: String,
    ) {
        sourceSets.forEach { sourceSet ->
            Konsist
                .scopeFromProject(moduleName = moduleName, sourceSetName = sourceSet)
                .classes()
                .assertTrue {
                    it.resideInPackage(packagePattern)
                }
        }
    }

    fun assertNoBlockedImports(
        moduleName: String,
        sourceSets: List<String>,
        blockedImportPrefixes: List<String>,
    ) {
        sourceSets.forEach { sourceSet ->
            Konsist
                .scopeFromProject(moduleName = moduleName, sourceSetName = sourceSet)
                .files
                .assertTrue { file ->
                    !hasBlockedImport(file, blockedImportPrefixes)
                }
        }
    }

    fun assertNoBlockedImportsInPackages(
        moduleName: String,
        sourceSets: List<String>,
        packageNames: List<String>,
        blockedImportPrefixes: List<String>,
    ) {
        sourceSets.forEach { sourceSet ->
            val packageFiles = Konsist
                .scopeFromProject(moduleName = moduleName, sourceSetName = sourceSet)
                .files
                .filter { file -> packageNames.any { packageName -> file.hasPackage(packageName) } }
            check(packageFiles.isNotEmpty()) {
                "No $moduleName/$sourceSet files matched packages: ${packageNames.joinToString()}"
            }
            packageFiles.assertTrue { file ->
                !hasBlockedImport(file, blockedImportPrefixes)
            }
        }
    }

    fun assertKotestOnly(
        moduleName: String,
        testSourceSets: List<String>,
    ) {
        testSourceSets.forEach { sourceSet ->
            Konsist
                .scopeFromProject(moduleName = moduleName, sourceSetName = sourceSet)
                .files
                .assertTrue { file ->
                    !hasBlockedImport(file, blockedTestFrameworkPrefixes)
                }
        }
    }

    fun hasBlockedImport(
        file: KoFileDeclaration,
        blockedImportPrefixes: List<String>,
    ): Boolean = file.hasImport { import ->
        importNameMatchesAnyBlockedPrefix(import.name, blockedImportPrefixes)
    }

    /**
     * The single matching rule behind every blocked-import guard: an import is blocked when its
     * fully-qualified name starts with any blocked prefix. [hasBlockedImport] delegates here so the
     * per-prefix coverage assertion below exercises the exact logic the production guard runs.
     */
    fun importNameMatchesAnyBlockedPrefix(
        importName: String,
        blockedImportPrefixes: List<String>,
    ): Boolean = blockedImportPrefixes.any { importName.startsWith(it) }

    /**
     * Proves every prefix in [blockedImportPrefixes] is load-bearing, closing the residual
     * vacuous-guard gap from #266: only a couple of prefixes per client module were ever exercised
     * by a concrete fixture, so a prefix that no constructible import can match, or one already
     * subsumed by a broader sibling, could sit in the list doing nothing with no test failing.
     *
     * For each prefix it builds a synthetic import name from that prefix and asserts:
     * - positive: the synthetic import is blocked by the full list (the prefix is matchable at all);
     * - necessity: removing only that prefix leaves the synthetic import unblocked (the prefix is the
     *   sole reason its namespace is guarded — not shadowed by a broader sibling, not a duplicate).
     *
     * If a genuine broader+narrower pair is ever added (e.g. `...cli.` alongside `...cli.input.`), the
     * narrower entry is redundant and this assertion SHOULD turn red. That is the intended dead-prefix
     * detection; do not special-case it — drop the redundant prefix instead.
     *
     * By construction this iterates the same list the guard uses (single source of truth), so it
     * cannot detect deleting a prefix from the list — the iteration simply shrinks. It detects a prefix
     * mutated to an unmatchable value, a prefix shadowed by a broader sibling, and a duplicated prefix.
     */
    fun assertEveryBlockedPrefixIsLoadBearing(blockedImportPrefixes: List<String>) {
        blockedImportPrefixes.shouldNotContainDuplicates()

        blockedImportPrefixes.forEachIndexed { index, prefix ->
            val syntheticImport = prefix + "Synthetic"

            withClue("blocked prefix '$prefix' matches no constructible import (dead guard)") {
                syntheticImport.shouldBeConstructibleImportName()
                importNameMatchesAnyBlockedPrefix(syntheticImport, blockedImportPrefixes).shouldBeTrue()
            }

            val listWithoutPrefix = blockedImportPrefixes.filterIndexed { i, _ -> i != index }
            withClue("blocked prefix '$prefix' is not load-bearing (shadowed by a broader sibling)") {
                importNameMatchesAnyBlockedPrefix(syntheticImport, listWithoutPrefix).shouldBeFalse()
            }
        }
    }

    private fun String.shouldBeConstructibleImportName() {
        split(".").all { segment ->
            segment.matches(qualifiedNameSegment)
        }.shouldBeTrue()
    }

    private val qualifiedNameSegment = Regex("[A-Za-z_][A-Za-z0-9_]*")
}

package com.yonatankarp.agentdesk.testfixtures.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import com.lemonappdev.konsist.api.verify.assertTrue

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
        blockedImportPrefixes.any { import.name.startsWith(it) }
    }
}

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

    /**
     * Asserts that production files residing in exactly [packageName] import nothing
     * matching [blockedImportPrefixes]. Used to pin one-directional layering between
     * sub-packages inside a module.
     */
    fun assertSubPackageDoesNotImport(
        moduleName: String,
        sourceSets: List<String>,
        packageName: String,
        blockedImportPrefixes: List<String>,
    ) {
        sourceSets.forEach { sourceSet ->
            Konsist
                .scopeFromProject(moduleName = moduleName, sourceSetName = sourceSet)
                .files
                .filter { it.packagee?.name == packageName }
                .assertTrue { file ->
                    !hasBlockedImport(file, blockedImportPrefixes)
                }
        }
    }

    /**
     * True when [file] imports a type declared directly in [packageName] — the prefix
     * matches and exactly one segment remains, so sub-packages of [packageName] do not
     * count. Used to forbid sub-packages from reaching back to root-package types.
     */
    fun hasImportFromExactPackage(
        file: KoFileDeclaration,
        packageName: String,
    ): Boolean = file.hasImport { import ->
        import.name.startsWith("$packageName.") &&
            !import.name.removePrefix("$packageName.").contains(".")
    }

    fun assertSubPackageDoesNotImportExactPackage(
        moduleName: String,
        sourceSets: List<String>,
        packageName: String,
        forbiddenExactPackage: String,
    ) {
        sourceSets.forEach { sourceSet ->
            Konsist
                .scopeFromProject(moduleName = moduleName, sourceSetName = sourceSet)
                .files
                .filter { it.packagee?.name == packageName }
                .assertTrue { file ->
                    !hasImportFromExactPackage(file, forbiddenExactPackage)
                }
        }
    }
}

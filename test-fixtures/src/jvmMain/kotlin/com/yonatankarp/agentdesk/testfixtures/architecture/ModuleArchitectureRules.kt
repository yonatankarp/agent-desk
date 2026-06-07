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
     * Asserts that production files residing in exactly [packageName] (descendant
     * packages are not matched) import nothing matching [blockedImportPrefixes].
     * Used to pin one-directional layering between sub-packages inside a module.
     * `strict = true` makes an empty filtered scope a hard failure instead of a
     * vacuous pass, so a renamed or emptied sub-package cannot silently disable
     * the rule. Import-based: fully-qualified inline references without an import
     * statement are not detected.
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
                .assertTrue(strict = true) { file ->
                    !hasBlockedImport(file, blockedImportPrefixes)
                }
        }
    }

    /**
     * True when [file] imports a type declared directly in [packageName] — the prefix
     * matches and exactly one segment remains, so sub-packages of [packageName] do not
     * count. Used to forbid sub-packages from reaching back to root-package types.
     * Import-based: fully-qualified inline references without an import statement are
     * not detected.
     */
    fun hasImportFromExactPackage(
        file: KoFileDeclaration,
        packageName: String,
    ): Boolean = file.hasImport { import ->
        import.name.startsWith("$packageName.") &&
            !import.name.removePrefix("$packageName.").contains(".")
    }

    /**
     * Asserts that production files residing in exactly [packageName] import no type
     * declared directly in [forbiddenExactPackage]. `strict = true` keeps an empty
     * filtered scope a hard failure (see [assertSubPackageDoesNotImport]).
     */
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
                .assertTrue(strict = true) { file ->
                    !hasImportFromExactPackage(file, forbiddenExactPackage)
                }
        }
    }
}

package com.yonatankarp.agentdesk.testfixtures.architecture

import io.kotest.core.spec.style.FunSpec

class ArchitectureKonsistTest :
    FunSpec({
        test("test-fixtures tests use Kotest only") {
            ModuleArchitectureRules.assertKotestOnly(
                moduleName = "test-fixtures",
                testSourceSets = listOf("commonTest", "jvmTest"),
            )
        }
    })

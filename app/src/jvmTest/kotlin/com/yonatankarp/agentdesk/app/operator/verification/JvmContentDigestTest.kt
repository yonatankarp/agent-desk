package com.yonatankarp.agentdesk.app.operator.verification

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.nio.file.Files

class JvmContentDigestTest :
    FunSpec({
        test("computes a SHA-256 content digest through the JVM adapter path") {
            val artifact = Files.createTempFile("agent-desk-artifact", ".txt")
            Files.writeString(artifact, "verified artifact\n")

            JvmContentDigest.sha256(artifact) shouldBe
                ContentDigest.parseSha256("b968f651d921bbdd1a3765457ef6ecaaf1cb0e2b0e525f1d92731d3f2e6bc886")
        }
    })

package com.yonatankarp.agentdesk.cli

import com.yonatankarp.agentdesk.testfixtures.matchers.shouldBePublicSafe
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.io.IOException
import java.io.InputStream

class CliEventReadsTest :
    BehaviorSpec({
        given("stdin event input") {
            `when`("the input stream fails to read") {
                then("the failure surfaces as the shared public-safe input error") {
                    val failingInput = object : InputStream() {
                        override fun read(): Int = throw IOException("disk gone")

                        override fun read(
                            b: ByteArray,
                            off: Int,
                            len: Int,
                        ): Int = throw IOException("disk gone")
                    }

                    val result = runCli("--stdin", input = failingInput)

                    result.exitCode shouldBe 1
                    result.error shouldContain "Event input could not be read."
                    result.error shouldNotContain "disk gone"
                    result.error.shouldBePublicSafe()
                    result.output shouldBe ""
                }
            }
        }
    })

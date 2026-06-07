package com.yonatankarp.agentdesk.cli

import com.yonatankarp.agentdesk.testfixtures.matchers.shouldBePublicSafe
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.nio.file.Files

class CliUsageErrorTest :
    BehaviorSpec({
        given("usage errors") {
            `when`("each invalid argument list is run") {
                then("it exits with code 2, explains the error, and prints nothing on stdout") {
                    usageErrorCases().forEach { case ->
                        val result = runCli(*case.args.toTypedArray())

                        withClue("args: ${case.args}") {
                            result.exitCode shouldBe 2
                            case.expectedErrors.forEach { expected ->
                                result.error shouldContain expected
                            }
                            result.error.shouldBePublicSafe()
                            result.output shouldBe ""
                        }
                    }
                }
            }
        }

        given("the help flags") {
            `when`("each is run") {
                then("help flags print usage and exit cleanly") {
                    listOf("--help", "-h").forEach { flag ->
                        val result = runCli(flag)

                        withClue("args: $flag") {
                            result.exitCode shouldBe 0
                            result.output shouldContain "Agent Desk"
                            result.output shouldContain "Usage:"
                            result.output shouldContain "--help          Show this help."
                            result.output.shouldBePublicSafe()
                            result.error shouldBe ""
                        }
                    }
                }
            }
        }

        given("a config combined with another input mode") {
            `when`("the CLI is run") {
                then("config cannot be combined with other input modes") {
                    val configFile = Files.createTempFile("agent-desk-cli-config", ".properties")

                    val result = runCli("--config", configFile.toString(), "--sample")

                    result.exitCode shouldBe 2
                    result.error shouldContain "Choose only one input mode."
                    result.error.shouldBePublicSafe()
                    result.output shouldBe ""
                }
            }
        }
    })

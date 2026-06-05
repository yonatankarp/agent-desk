package com.yonatankarp.agentdesk.core.domain.valueobjects

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

class WorkItemIdTest :
    BehaviorSpec({
        given("a work item id") {
            `when`("raw input has surrounding whitespace and mixed case") {
                then("it normalizes whitespace and case") {
                    val id = WorkItemId.parse("  Agent-Task:42  ")

                    id.value shouldBe "agent-task:42"
                    id.toString() shouldBe "agent-task:42"
                }
            }

            `when`("raw input is blank") {
                then("it rejects the value") {
                    shouldThrow<IllegalArgumentException> {
                        WorkItemId.parse("   ")
                    }
                }
            }

            `when`("raw input has unsupported characters") {
                then("it rejects the value") {
                    shouldThrow<IllegalArgumentException> {
                        WorkItemId.parse("agent task")
                    }
                }
            }

            `when`("raw input looks like a private runtime identifier") {
                then("it rejects the value without echoing it") {
                    val rawIdentifier = "123456789" + "012345678"
                    val channelId = "channel:$rawIdentifier"
                    val sessionId = "session:local-agent"

                    val rawError = shouldThrow<IllegalArgumentException> {
                        WorkItemId.parse(rawIdentifier)
                    }

                    val channelError = shouldThrow<IllegalArgumentException> {
                        WorkItemId.parse(channelId)
                    }
                    val sessionError = shouldThrow<IllegalArgumentException> {
                        WorkItemId.parse(sessionId)
                    }

                    rawError.message shouldContain "raw channel or message identifiers"
                    rawError.message.orEmpty() shouldNotContain rawIdentifier
                    channelError.message shouldContain "private runtime"
                    channelError.message.orEmpty() shouldNotContain channelId
                    sessionError.message shouldContain "private runtime"
                    sessionError.message.orEmpty() shouldNotContain sessionId
                }
            }
        }
    })

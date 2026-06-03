package com.yonatankarp.agentdesk.core.domain.valueobjects

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

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
        }
    })

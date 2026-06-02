package com.yonatankarp.agentdesk.core

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class WorkItemIdTest :
    FunSpec({
        test("normalizes whitespace and case") {
            val id = WorkItemId.parse("  Agent-Task:42  ")

            id.value shouldBe "agent-task:42"
            id.toString() shouldBe "agent-task:42"
        }

        test("rejects blank ids") {
            shouldThrow<IllegalArgumentException> {
                WorkItemId.parse("   ")
            }
        }

        test("rejects unsupported characters") {
            shouldThrow<IllegalArgumentException> {
                WorkItemId.parse("agent task")
            }
        }
    })

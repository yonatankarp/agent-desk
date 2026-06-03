package com.yonatankarp.agentdesk.core.domain.events

import com.yonatankarp.agentdesk.core.fixtures.CoreFixtures
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class WorkEventTest :
    FunSpec({
        test("work started event exposes envelope type from payload") {
            val event = CoreFixtures.workStartedEvent()

            event.type shouldBe WorkEventType.WorkStarted
            event.type.wireName shouldBe "work.started"
        }

        test("work blocked event carries sanitized reason payload") {
            val event = CoreFixtures.workBlockedEvent()

            event.type shouldBe WorkEventType.WorkBlocked
            event.payload shouldBe WorkBlockedPayload(reason = CoreFixtures.blockedReason)
        }

        test("event identifiers and sources normalize case") {
            WorkEventId.parse("  Event:Agent-Task:42:Started  ").value shouldBe
                "event:agent-task:42:started"
            EventSource.parse("  Mock-Adapter  ").value shouldBe "mock-adapter"
        }

        test("event identifiers and sources reject unsupported characters") {
            shouldThrow<IllegalArgumentException> {
                WorkEventId.parse("event agent task")
            }
            shouldThrow<IllegalArgumentException> {
                EventSource.parse("mock adapter")
            }
        }

        test("event timestamps require UTC instants") {
            shouldThrow<IllegalArgumentException> {
                EventTimestamp.parse("2026-06-02 21:00:00")
            }
            shouldThrow<IllegalArgumentException> {
                EventTimestamp.parse("2026-06-02T21:00:00+02:00")
            }
        }
    })

package com.yonatankarp.agentdesk.core.domain.events

import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemId
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemTitle
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkSummary
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class WorkEventTest :
    FunSpec({
        test("work started event exposes envelope type from payload") {
            val event = WorkEvent(
                id = WorkEventId.parse("event:agent-task:42:started"),
                occurredAt = EventTimestamp.parse("2026-06-02T21:00:00Z"),
                source = EventSource.parse("mock-adapter"),
                workItemId = WorkItemId.parse("agent-task:42"),
                payload = WorkStartedPayload(
                    title = WorkItemTitle.parse("Run public hygiene check"),
                    summary = WorkSummary.parse("Agent accepted the task and started local checks."),
                ),
            )

            event.type shouldBe WorkEventType.WorkStarted
            event.type.wireName shouldBe "work.started"
        }

        test("work blocked event carries sanitized reason payload") {
            val event = WorkEvent(
                id = WorkEventId.parse("event:agent-task:42:blocked"),
                occurredAt = EventTimestamp.parse("2026-06-02T21:05:00.123Z"),
                source = EventSource.parse("mock-adapter"),
                workItemId = WorkItemId.parse("agent-task:42"),
                payload = WorkBlockedPayload(
                    reason = WorkSummary.parse("CI failed on the core test task."),
                ),
            )

            event.type shouldBe WorkEventType.WorkBlocked
            event.payload shouldBe WorkBlockedPayload(
                reason = WorkSummary.parse("CI failed on the core test task."),
            )
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

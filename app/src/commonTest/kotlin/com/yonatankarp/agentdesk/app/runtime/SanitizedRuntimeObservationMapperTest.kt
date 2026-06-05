package com.yonatankarp.agentdesk.app.runtime

import com.yonatankarp.agentdesk.core.domain.events.WorkBlockedPayload
import com.yonatankarp.agentdesk.core.domain.events.WorkCanceledPayload
import com.yonatankarp.agentdesk.core.domain.events.WorkFailedPayload
import com.yonatankarp.agentdesk.core.domain.events.WorkNeedsDecisionPayload
import com.yonatankarp.agentdesk.core.domain.events.WorkStartedPayload
import com.yonatankarp.agentdesk.core.domain.events.WorkSucceededPayload
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

class SanitizedRuntimeObservationMapperTest :
    BehaviorSpec({
        val mapper = SanitizedRuntimeObservationMapper()

        given("sanitized runtime observations") {
            `when`("mock lifecycle observations are mapped") {
                then("they become canonical work events") {
                    val events = MockRuntimeWorkEventSource().loadObservations().map(mapper::toWorkEvent)

                    events.map { it.type.wireName }.shouldContainExactly(
                        "work.started",
                        "work.started",
                        "work.blocked",
                        "work.started",
                        "work.needs-decision",
                        "work.succeeded",
                    )
                    assertSoftly(events.first()) {
                        id.toString() shouldBe "event:agent-task:42:started"
                        source.toString() shouldBe "mock-adapter"
                        workItemId.toString() shouldBe "agent-task:42"
                        (payload as WorkStartedPayload).title.toString() shouldBe "Run public hygiene check"
                    }
                    assertSoftly(events.last()) {
                        id.toString() shouldBe "event:agent-task:42:succeeded"
                        payload shouldBe WorkSucceededPayload
                    }
                }
            }

            `when`("each supported observation kind is mapped") {
                then("the mapper covers every canonical work event type") {
                    val observations = listOf(
                        RuntimeWorkObservation(
                            eventId = "event:agent-task:51:started",
                            occurredAt = "2026-06-02T21:00:00Z",
                            source = "mock-adapter",
                            workItemId = "agent-task:51",
                            kind = RuntimeWorkObservationKind.Started,
                            title = "Run public hygiene check",
                            summary = "Agent accepted the task and started local checks.",
                        ),
                        RuntimeWorkObservation(
                            eventId = "event:agent-task:51:needs-decision",
                            occurredAt = "2026-06-02T21:05:00Z",
                            source = "mock-adapter",
                            workItemId = "agent-task:51",
                            kind = RuntimeWorkObservationKind.NeedsDecision,
                            reason = "Operator must choose whether to retry the failed check.",
                        ),
                        RuntimeWorkObservation(
                            eventId = "event:agent-task:51:blocked",
                            occurredAt = "2026-06-02T21:10:00Z",
                            source = "mock-adapter",
                            workItemId = "agent-task:51",
                            kind = RuntimeWorkObservationKind.Blocked,
                            reason = "CI failed on the core test task.",
                        ),
                        RuntimeWorkObservation(
                            eventId = "event:agent-task:51:succeeded",
                            occurredAt = "2026-06-02T21:15:00Z",
                            source = "mock-adapter",
                            workItemId = "agent-task:51",
                            kind = RuntimeWorkObservationKind.Succeeded,
                        ),
                        RuntimeWorkObservation(
                            eventId = "event:agent-task:51:failed",
                            occurredAt = "2026-06-02T21:20:00Z",
                            source = "mock-adapter",
                            workItemId = "agent-task:51",
                            kind = RuntimeWorkObservationKind.Failed,
                            reason = "Build failed after retry.",
                        ),
                        RuntimeWorkObservation(
                            eventId = "event:agent-task:51:canceled",
                            occurredAt = "2026-06-02T21:25:00Z",
                            source = "mock-adapter",
                            workItemId = "agent-task:51",
                            kind = RuntimeWorkObservationKind.Canceled,
                            reason = "Operator canceled the task.",
                        ),
                    )

                    val payloads = observations.map { mapper.toWorkEvent(it).payload }

                    payloads.map { it.type.wireName }.shouldContainExactly(
                        "work.started",
                        "work.needs-decision",
                        "work.blocked",
                        "work.succeeded",
                        "work.failed",
                        "work.canceled",
                    )
                    assertSoftly {
                        (payloads[0] as WorkStartedPayload).title.toString() shouldBe "Run public hygiene check"
                        (payloads[1] as WorkNeedsDecisionPayload).reason.toString() shouldBe
                            "Operator must choose whether to retry the failed check."
                        (payloads[2] as WorkBlockedPayload).reason.toString() shouldBe
                            "CI failed on the core test task."
                        payloads[3] shouldBe WorkSucceededPayload
                        (payloads[4] as WorkFailedPayload).reason.toString() shouldBe "Build failed after retry."
                        (payloads[5] as WorkCanceledPayload).reason.toString() shouldBe "Operator canceled the task."
                    }
                }
            }

            `when`("a canceled observation has no reason") {
                then("the mapper keeps the reason optional") {
                    val event = mapper.toWorkEvent(
                        RuntimeWorkObservation(
                            eventId = "event:agent-task:52:canceled",
                            occurredAt = "2026-06-02T21:30:00Z",
                            source = "mock-adapter",
                            workItemId = "agent-task:52",
                            kind = RuntimeWorkObservationKind.Canceled,
                        ),
                    )

                    (event.payload as WorkCanceledPayload).reason shouldBe null
                }
            }
        }

        given("unsafe runtime observations") {
            `when`("a runtime field includes private local details") {
                then("the mapper rejects it before constructing a work event") {
                    val error = shouldThrow<IllegalArgumentException> {
                        mapper.toWorkEvent(
                            RuntimeWorkObservation(
                                eventId = "event:agent-task:99:blocked",
                                occurredAt = "2026-06-02T21:30:00Z",
                                source = "mock-adapter",
                                workItemId = "agent-task:99",
                                kind = RuntimeWorkObservationKind.Blocked,
                                reason = "Read failed at ${privateLinuxPath("private-token.txt")}",
                            ),
                        )
                    }

                    error.message shouldContain "Runtime observation reason"
                    error.message.orEmpty() shouldNotContain privateLinuxPath("private-token.txt")
                }
            }

            `when`("runtime ids include raw channel or session identifiers") {
                then("the mapper rejects them before projection without echoing the unsafe id") {
                    val rawIdentifier = "123456789" + "012345678"
                    val unsafeEventId = "event:message:$rawIdentifier:started"
                    val unsafeWorkItemId = "session:local-agent"
                    val unsafeThreadSource = "thread:local-review"

                    val error = shouldThrow<IllegalArgumentException> {
                        mapper.toWorkEvent(
                            RuntimeWorkObservation(
                                eventId = unsafeEventId,
                                occurredAt = "2026-06-02T21:30:00Z",
                                source = "mock-adapter",
                                workItemId = "agent-task:99",
                                kind = RuntimeWorkObservationKind.Started,
                                title = "Run public hygiene check",
                            ),
                        )
                    }

                    assertSoftly(error.message.orEmpty()) {
                        shouldContain("Runtime observation eventId")
                        shouldContain("private runtime")
                        shouldNotContain(unsafeEventId)
                        shouldNotContain(rawIdentifier)
                    }

                    val workItemError = shouldThrow<IllegalArgumentException> {
                        mapper.toWorkEvent(
                            RuntimeWorkObservation(
                                eventId = "event:agent-task:99:started",
                                occurredAt = "2026-06-02T21:30:00Z",
                                source = "mock-adapter",
                                workItemId = unsafeWorkItemId,
                                kind = RuntimeWorkObservationKind.Started,
                                title = "Run public hygiene check",
                            ),
                        )
                    }

                    val sourceError = shouldThrow<IllegalArgumentException> {
                        mapper.toWorkEvent(
                            RuntimeWorkObservation(
                                eventId = "event:agent-task:99:started",
                                occurredAt = "2026-06-02T21:30:00Z",
                                source = unsafeThreadSource,
                                workItemId = "agent-task:99",
                                kind = RuntimeWorkObservationKind.Started,
                                title = "Run public hygiene check",
                            ),
                        )
                    }

                    assertSoftly(workItemError.message.orEmpty()) {
                        shouldContain("Runtime observation workItemId")
                        shouldContain("private runtime")
                        shouldNotContain(unsafeWorkItemId)
                    }

                    assertSoftly(sourceError.message.orEmpty()) {
                        shouldContain("Runtime observation source")
                        shouldContain("private runtime")
                        shouldNotContain(unsafeThreadSource)
                    }
                }
            }

            `when`("a started observation is missing its title") {
                then("the mapper rejects the incomplete payload") {
                    val error = shouldThrow<IllegalArgumentException> {
                        mapper.toWorkEvent(
                            RuntimeWorkObservation(
                                eventId = "event:agent-task:99:started",
                                occurredAt = "2026-06-02T21:30:00Z",
                                source = "mock-adapter",
                                workItemId = "agent-task:99",
                                kind = RuntimeWorkObservationKind.Started,
                            ),
                        )
                    }

                    error.message shouldContain "title"
                }
            }

            `when`("an attention observation is missing its reason") {
                then("the mapper rejects the incomplete payload") {
                    val error = shouldThrow<IllegalArgumentException> {
                        mapper.toWorkEvent(
                            RuntimeWorkObservation(
                                eventId = "event:agent-task:99:needs-decision",
                                occurredAt = "2026-06-02T21:30:00Z",
                                source = "mock-adapter",
                                workItemId = "agent-task:99",
                                kind = RuntimeWorkObservationKind.NeedsDecision,
                            ),
                        )
                    }

                    error.message shouldContain "reason"
                }
            }

            `when`("a failed observation is missing its reason") {
                then("the mapper rejects the incomplete payload") {
                    val error = shouldThrow<IllegalArgumentException> {
                        mapper.toWorkEvent(
                            RuntimeWorkObservation(
                                eventId = "event:agent-task:99:failed",
                                occurredAt = "2026-06-02T21:30:00Z",
                                source = "mock-adapter",
                                workItemId = "agent-task:99",
                                kind = RuntimeWorkObservationKind.Failed,
                            ),
                        )
                    }

                    error.message shouldContain "reason"
                }
            }

            `when`("renderer-facing observation text includes private boundary details") {
                then("the mapper rejects it without echoing the unsafe value") {
                    val unsafeReason = listOf("raw", "transcript marker").joinToString(" ")

                    val error = shouldThrow<IllegalArgumentException> {
                        mapper.toWorkEvent(
                            RuntimeWorkObservation(
                                eventId = "event:agent-task:99:failed",
                                occurredAt = "2026-06-02T21:30:00Z",
                                source = "mock-adapter",
                                workItemId = "agent-task:99",
                                kind = RuntimeWorkObservationKind.Failed,
                                reason = unsafeReason,
                            ),
                        )
                    }

                    assertSoftly(error.message.orEmpty()) {
                        shouldContain("Runtime observation reason")
                        shouldNotContain(unsafeReason)
                    }
                }
            }
        }

        given("mock runtime adapter fixtures") {
            `when`("the source emits example events") {
                then("fixture output stays public-safe") {
                    val text = MockRuntimeWorkEventSource().loadObservations().joinToString("\n") { observation ->
                        "${observation.eventId} ${observation.occurredAt} ${observation.source} " +
                            "${observation.workItemId} ${observation.kind} " +
                            "${observation.title} ${observation.summary} ${observation.reason}"
                    }

                    assertSoftly {
                        text shouldContain "mock-adapter"
                        text shouldNotContain "/home/"
                        text shouldNotContain "discord"
                        text shouldNotContain "token"
                        text shouldNotContain "secret"
                        text shouldNotContain "op://"
                    }
                }
            }
        }
    })

private fun privateLinuxPath(fileName: String): String = "/home/" + "operator/$fileName"

package com.yonatankarp.agentdesk.app.runtime

import com.yonatankarp.agentdesk.core.domain.events.WorkBlockedPayload
import com.yonatankarp.agentdesk.core.domain.events.WorkStartedPayload
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
            `when`("started and blocked observations are mapped") {
                then("they become canonical work events") {
                    val events = MockRuntimeWorkEventSource(mapper).loadEvents()

                    events.map { it.type.wireName }.shouldContainExactly("work.started", "work.blocked")
                    assertSoftly(events.first()) {
                        id.toString() shouldBe "event:agent-task:42:started"
                        source.toString() shouldBe "mock-adapter"
                        workItemId.toString() shouldBe "agent-task:42"
                        (payload as WorkStartedPayload).title.toString() shouldBe "Run public hygiene check"
                    }
                    assertSoftly(events.last()) {
                        id.toString() shouldBe "event:agent-task:44:blocked"
                        (payload as WorkBlockedPayload).reason.toString() shouldBe "CI failed on the core test task."
                    }
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
                                reason = "Read failed at /home/operator/private-token.txt",
                            ),
                        )
                    }

                    error.message shouldContain "public-safe"
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
        }

        given("mock runtime adapter fixtures") {
            `when`("the source emits example events") {
                then("fixture output stays public-safe") {
                    val text = MockRuntimeWorkEventSource().loadEvents().joinToString("\n") { event ->
                        "${event.id} ${event.occurredAt} ${event.source} ${event.workItemId} ${event.payload}"
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

package com.yonatankarp.agentdesk.core.domain.events

import com.yonatankarp.agentdesk.core.fixtures.CoreFixtures
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class WorkEventTest :
    BehaviorSpec({
        given("a work started event") {
            `when`("the envelope type is read") {
                then("it exposes the payload type") {
                    val event = CoreFixtures.workStartedEvent()

                    event.type shouldBe WorkEventType.WorkStarted
                    event.type.wireName shouldBe "work.started"
                }
            }
        }

        given("a work blocked event") {
            `when`("the payload is inspected") {
                then("it carries the sanitized reason") {
                    val event = CoreFixtures.workBlockedEvent()

                    event.type shouldBe WorkEventType.WorkBlocked
                    event.payload shouldBe WorkBlockedPayload(reason = CoreFixtures.blockedReason)
                }
            }
        }

        given("additional lifecycle events") {
            `when`("their envelope types are read") {
                then("they expose stable wire names") {
                    CoreFixtures.workNeedsDecisionEvent().type.wireName shouldBe "work.needs-decision"
                    CoreFixtures.workSucceededEvent().type.wireName shouldBe "work.succeeded"
                    CoreFixtures.workFailedEvent().type.wireName shouldBe "work.failed"
                    CoreFixtures.workCanceledEvent().type.wireName shouldBe "work.canceled"
                }
            }
        }

        given("event identifiers and sources") {
            `when`("raw values use surrounding whitespace and mixed case") {
                then("they normalize case") {
                    WorkEventId.parse("  Event:Agent-Task:42:Started  ").value shouldBe
                        "event:agent-task:42:started"
                    EventSource.parse("  Mock-Adapter  ").value shouldBe "mock-adapter"
                }
            }

            `when`("raw values include unsupported characters") {
                then("they reject the values") {
                    shouldThrow<IllegalArgumentException> {
                        WorkEventId.parse("event agent task")
                    }
                    shouldThrow<IllegalArgumentException> {
                        EventSource.parse("mock adapter")
                    }
                }
            }
        }

        given("event timestamps") {
            `when`("raw values are not UTC instants") {
                then("they reject the values") {
                    shouldThrow<IllegalArgumentException> {
                        EventTimestamp.parse("2026-06-02 21:00:00")
                    }

                    shouldThrow<IllegalArgumentException> {
                        EventTimestamp.parse("2026-06-02T21:00:00+02:00")
                    }
                }
            }
        }
    })

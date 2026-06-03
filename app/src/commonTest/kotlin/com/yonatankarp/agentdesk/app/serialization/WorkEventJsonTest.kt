package com.yonatankarp.agentdesk.app.serialization

import com.yonatankarp.agentdesk.app.fixtures.AppFixtures
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class WorkEventJsonTest :
    BehaviorSpec({
        given("a work started event") {
            `when`("it is encoded") {
                then("the JSON uses deterministic public wire names") {
                    WorkEventJson.encode(AppFixtures.workStartedEvent()) shouldBe
                        """{"id":"event:agent-task:42:started","occurredAt":"2026-06-02T21:00:00Z","source":"mock-adapter","workItemId":"agent-task:42","type":"work.started","payload":{"title":"Run public hygiene check","summary":"Agent accepted the task and started local checks."}}"""
                }
            }

            `when`("it is decoded after encoding") {
                then("it round trips through the serialized record") {
                    val event = AppFixtures.workStartedEvent()

                    WorkEventJson.decode(WorkEventJson.encode(event)) shouldBe event
                }
            }
        }

        given("a work blocked event") {
            `when`("it is decoded after encoding") {
                then("it round trips through the serialized record") {
                    val event = AppFixtures.workBlockedEvent()

                    WorkEventJson.decode(WorkEventJson.encode(event)) shouldBe event
                }
            }
        }

        given("current lifecycle payload variants") {
            `when`("they are converted to records") {
                then("they preserve stable wire names without Kotlin class names") {
                    listOf(
                        AppFixtures.workStartedEvent(),
                        AppFixtures.workNeedsDecisionEvent(),
                        AppFixtures.workBlockedEvent(),
                        AppFixtures.workSucceededEvent(),
                        AppFixtures.workFailedEvent(),
                        AppFixtures.workCanceledEvent(),
                    ).map { WorkEventJson.toRecord(it).type } shouldBe
                        listOf(
                            "work.started",
                            "work.needs-decision",
                            "work.blocked",
                            "work.succeeded",
                            "work.failed",
                            "work.canceled",
                        )
                }
            }
        }

        given("malformed event records") {
            `when`("the event timestamp is not a UTC instant") {
                then("decoding rejects the record") {
                    val error = shouldThrow<IllegalArgumentException> {
                        WorkEventJson.decode(
                            """{"id":"event:agent-task:42:started","occurredAt":"2026-06-02 21:00:00","source":"mock-adapter","workItemId":"agent-task:42","type":"work.started","payload":{"title":"Run public hygiene check"}}""",
                        )
                    }

                    error.message shouldContain "timestamp"
                }
            }

            `when`("the event source is not adapter neutral") {
                then("decoding rejects the record") {
                    val error = shouldThrow<IllegalArgumentException> {
                        WorkEventJson.decode(
                            """{"id":"event:agent-task:42:started","occurredAt":"2026-06-02T21:00:00Z","source":"mock adapter","workItemId":"agent-task:42","type":"work.started","payload":{"title":"Run public hygiene check"}}""",
                        )
                    }

                    error.message shouldContain "source"
                }
            }

            `when`("the event type is unknown") {
                then("decoding rejects the record") {
                    val error = shouldThrow<IllegalArgumentException> {
                        WorkEventJson.decode(
                            """{"id":"event:agent-task:42:paused","occurredAt":"2026-06-02T21:00:00Z","source":"mock-adapter","workItemId":"agent-task:42","type":"work.paused","payload":{}}""",
                        )
                    }

                    error.message shouldContain "Unknown work event type"
                }
            }
        }
    })

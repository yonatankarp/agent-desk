package com.yonatankarp.agentdesk.app.serialization

import com.yonatankarp.agentdesk.app.fixtures.AppFixtures
import com.yonatankarp.agentdesk.testfixtures.checkRunEvidence
import com.yonatankarp.agentdesk.testfixtures.commitEvidence
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

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

        given("evidence references") {
            `when`("an event with evidence is encoded and decoded") {
                then("it preserves compact public-safe evidence records") {
                    val event = AppFixtures.workStartedEvent().copy(
                        evidenceReferences = listOf(
                            commitEvidence("Implementation commit", "commit:80de32988617392e1f42e6c4c48c66a56aaae4c4"),
                            checkRunEvidence(
                                "Gradle Build",
                                "https://github.com/yonatankarp/agent-desk/actions/runs/26937983933",
                            ),
                        ),
                    )

                    val encoded = WorkEventJson.encode(event)

                    encoded shouldContain """"evidenceReferences":[{"kind":"commit","label":"Implementation commit","target":"commit:80de32988617392e1f42e6c4c48c66a56aaae4c4"}"""
                    WorkEventJson.decode(encoded) shouldBe event
                }
            }

            `when`("an older event omits evidence references") {
                then("it decodes with an empty evidence list") {
                    val event =
                        WorkEventJson.decode(
                            """{"id":"event:agent-task:42:started","occurredAt":"2026-06-02T21:00:00Z","source":"mock-adapter","workItemId":"agent-task:42","type":"work.started","payload":{"title":"Run public hygiene check"}}""",
                        )

                    event.evidenceReferences shouldBe emptyList()
                }
            }

            `when`("an event includes an unsafe evidence URL") {
                then("decoding rejects it without echoing the target") {
                    val unsafeTarget = "https://github.com@127.0.0.1/report"

                    val error = shouldThrow<IllegalArgumentException> {
                        WorkEventJson.decode(
                            """{"id":"event:agent-task:42:started","occurredAt":"2026-06-02T21:00:00Z","source":"mock-adapter","workItemId":"agent-task:42","type":"work.started","payload":{"title":"Run public hygiene check"},"evidenceReferences":[{"kind":"check-run","label":"Unsafe target","target":"$unsafeTarget"}]}""",
                        )
                    }

                    assertSoftly(error.message.orEmpty()) {
                        shouldContain("Evidence target")
                        shouldNotContain(unsafeTarget)
                    }
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
                then("decoding rejects the record without echoing the raw type") {
                    val rawType = "message:" + "123456789" + "012345678"

                    val error = shouldThrow<IllegalArgumentException> {
                        WorkEventJson.decode(
                            """{"id":"event:agent-task:42:paused","occurredAt":"2026-06-02T21:00:00Z","source":"mock-adapter","workItemId":"agent-task:42","type":"$rawType","payload":{}}""",
                        )
                    }

                    assertSoftly(error.message.orEmpty()) {
                        shouldContain("Unknown work event type")
                        shouldNotContain(rawType)
                    }
                }
            }

            `when`("the evidence kind is unknown") {
                then("decoding rejects the record without echoing the raw kind") {
                    val rawKind = "message:" + "123456789" + "012345678"
                    val error = shouldThrow<IllegalArgumentException> {
                        WorkEventJson.decode(
                            """{"id":"event:agent-task:42:started","occurredAt":"2026-06-02T21:00:00Z","source":"mock-adapter","workItemId":"agent-task:42","type":"work.started","payload":{"title":"Run public hygiene check"},"evidenceReferences":[{"kind":"$rawKind","label":"Raw log","target":"commit:80de32988617392e1f42e6c4c48c66a56aaae4c4"}]}""",
                        )
                    }

                    assertSoftly(error.message.orEmpty()) {
                        shouldContain("Unknown evidence reference kind")
                        shouldNotContain(rawKind)
                    }
                }
            }

            `when`("a renderer-facing payload contains unsafe text") {
                then("decoding rejects it without echoing the raw value") {
                    val unsafeTitle = listOf("Open", "Claw runtime context").joinToString("")

                    val error = shouldThrow<IllegalArgumentException> {
                        WorkEventJson.decode(
                            """{"id":"event:agent-task:42:started","occurredAt":"2026-06-02T21:00:00Z","source":"mock-adapter","workItemId":"agent-task:42","type":"work.started","payload":{"title":"$unsafeTitle"}}""",
                        )
                    }

                    assertSoftly(error.message.orEmpty()) {
                        shouldContain("Work item title")
                        shouldNotContain(unsafeTitle)
                    }
                }
            }
        }
    })

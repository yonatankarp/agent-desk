package com.yonatankarp.agentdesk.core.domain.events

import com.yonatankarp.agentdesk.core.fixtures.CoreFixtures
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

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

        given("evidence references") {
            `when`("public-safe evidence is parsed") {
                then("it preserves adapter-neutral kind, label, and target") {
                    val reference = EvidenceReference(
                        kind = EvidenceReferenceKind.CheckRun,
                        label = EvidenceLabel.parse("Gradle Build"),
                        target = EvidenceTarget.parse("https://github.com/yonatankarp/agent-desk/actions/runs/26937983933"),
                    )

                    reference.kind.wireName shouldBe "check-run"
                    reference.label.toString() shouldBe "Gradle Build"
                    reference.target.toString() shouldBe
                        "https://github.com/yonatankarp/agent-desk/actions/runs/26937983933"
                }
            }

            `when`("evidence points at private or unsafe material") {
                then("it rejects the reference text") {
                    shouldThrow<IllegalArgumentException> {
                        EvidenceTarget.parse("/" + "home/yonatan/.openclaw/private.log")
                    }.message shouldContain "private local paths"

                    shouldThrow<IllegalArgumentException> {
                        EvidenceTarget.parse("channel:" + "151144681" + "8880225483")
                    }.message shouldContain "private runtime"

                    shouldThrow<IllegalArgumentException> {
                        EvidenceLabel.parse("raw transcript excerpt")
                    }.message shouldContain "private runtime"

                    shouldThrow<IllegalArgumentException> {
                        EvidenceTarget.parse("https://localhost:8080/report")
                    }.message shouldContain "private hosts"

                    shouldThrow<IllegalArgumentException> {
                        EvidenceTarget.parse("github_pat_1234567890")
                    }.message shouldContain "secrets"
                }
            }
        }
    })

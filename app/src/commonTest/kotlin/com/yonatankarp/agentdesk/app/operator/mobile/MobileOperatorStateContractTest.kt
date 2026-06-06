package com.yonatankarp.agentdesk.app.operator.mobile

import com.yonatankarp.agentdesk.app.fixtures.AppFixtures
import com.yonatankarp.agentdesk.app.operator.StatusTone
import com.yonatankarp.agentdesk.app.serialization.WorkEventJson
import com.yonatankarp.agentdesk.core.domain.events.WorkEventId
import com.yonatankarp.agentdesk.testfixtures.checkRunEvidence
import com.yonatankarp.agentdesk.testfixtures.eventTimestampAt
import com.yonatankarp.agentdesk.testfixtures.workEvents
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

class MobileOperatorStateContractTest :
    BehaviorSpec({
        given("sample operator state") {
            `when`("the mobile contract is derived") {
                then("it exposes current work and human attention without adapter details") {
                    val state = MobileOperatorStateContract.sample()

                    assertSoftly {
                        state.currentWork.map { it.id }.shouldContainExactly(
                            "agent-task:42",
                            "agent-task:43",
                            "agent-task:44",
                        )
                        state.attentionQueue.map { it.workItem.id }.shouldContainExactly("agent-task:43", "agent-task:44")
                        state.attentionQueue.map { it.workItem.status.label }
                            .shouldContainExactly("Needs decision", "Blocked")
                        state.projectionWarnings shouldBe emptyList()
                    }
                }
            }
        }

        given("stored events with attention and evidence") {
            `when`("the mobile contract is derived from events") {
                then("it preserves status presentation and compact public-safe evidence references") {
                    val state = MobileOperatorStateContract.fromEvents(
                        workEvents {
                            started()
                            needsDecision(
                                evidence = listOf(
                                    checkRunEvidence(
                                        "Mobile contract check",
                                        "https://github.com/yonatankarp/agent-desk/actions/runs/26937983933",
                                    ),
                                ),
                            )
                        },
                    )

                    assertSoftly {
                        state.currentWork.single().status shouldBe
                            MobileStatusPresentation(label = "Needs decision", tone = StatusTone.Attention)
                        state.currentWork.single().evidenceReferences.single() shouldBe
                            MobileEvidenceReference(
                                kind = "check-run",
                                label = "Mobile contract check",
                                target = "https://github.com/yonatankarp/agent-desk/actions/runs/26937983933",
                            )
                        state.attentionQueue.single().reason shouldBe "Operator decision needed."
                        state.recentEvents.last().evidenceReferences.single().kind shouldBe "check-run"
                    }
                }
            }
        }

        given("stored events with stale running work") {
            `when`("a newer accepted event is past the stale threshold") {
                then("stale attention is included in the mobile attention queue") {
                    val state = MobileOperatorStateContract.fromEvents(
                        workEvents {
                            started()
                            started(
                                workItemId = "agent-task:77",
                                at = eventTimestampAt(minute = 1, hour = 22),
                                title = "Refresh operator summary",
                                summary = "Agent started a later task.",
                            )
                        },
                    )

                    val stale = state.attentionQueue.single { it.workItem.id == "agent-task:42" }

                    assertSoftly {
                        stale.workItem.status.label shouldBe "Running"
                        stale.stale shouldBe MobileStaleAttention(
                            lastEventAt = "2026-06-02T21:00:00Z",
                            staleForMinutes = 61,
                        )
                    }
                }
            }
        }

        given("stored events with a projection warning") {
            `when`("an invalid transition follows accepted state") {
                then("accepted current work and public-safe warning details are both exposed") {
                    val state = MobileOperatorStateContract.fromEvents(
                        workEvents {
                            started()
                            succeeded()
                            event(
                                AppFixtures.workBlockedEvent(
                                    id = WorkEventId.parse("event:agent-task:42:blocked-after-success"),
                                ),
                            )
                        },
                    )

                    assertSoftly {
                        state.currentWork shouldBe emptyList()
                        state.projectionWarnings.single() shouldBe
                            MobileProjectionWarning(
                                eventId = "event:agent-task:42:blocked-after-success",
                                reason = "Cannot transition work item agent-task:42 from Succeeded to Blocked",
                            )
                    }
                }
            }
        }

        given("stored events with unsafe runtime identifiers") {
            `when`("the mobile contract input is decoded") {
                then("message-like ids are rejected before mobile projection without echoing them") {
                    val rawIdentifier = "123456789" + "012345678"
                    val unsafeEventId = "event:message:$rawIdentifier:started"
                    val unsafeEvent =
                        """{"id":"$unsafeEventId","occurredAt":"2026-06-02T21:00:00Z","source":"mock-adapter",""" +
                            "\"workItemId\":\"agent-task:42\",\"type\":\"work.started\"," +
                            "\"payload\":{\"title\":\"Run public hygiene check\"}}"

                    val error = shouldThrow<IllegalArgumentException> {
                        MobileOperatorStateContract.fromEvents(
                            listOf(
                                WorkEventJson.decode(unsafeEvent),
                            ),
                        )
                    }

                    assertSoftly(error.message.orEmpty()) {
                        shouldContain("Work event id")
                        shouldContain("private runtime")
                        shouldNotContain(unsafeEventId)
                        shouldNotContain(rawIdentifier)
                    }
                }
            }
        }
    })

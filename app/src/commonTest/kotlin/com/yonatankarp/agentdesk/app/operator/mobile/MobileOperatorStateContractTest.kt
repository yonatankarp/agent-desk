package com.yonatankarp.agentdesk.app.operator.mobile

import com.yonatankarp.agentdesk.app.fixtures.AppFixtures
import com.yonatankarp.agentdesk.core.domain.events.EventTimestamp
import com.yonatankarp.agentdesk.core.domain.events.EvidenceLabel
import com.yonatankarp.agentdesk.core.domain.events.EvidenceReference
import com.yonatankarp.agentdesk.core.domain.events.EvidenceReferenceKind
import com.yonatankarp.agentdesk.core.domain.events.EvidenceTarget
import com.yonatankarp.agentdesk.core.domain.events.WorkEventId
import com.yonatankarp.agentdesk.core.domain.events.WorkStartedPayload
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemId
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemTitle
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkSummary
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

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
                        listOf(
                            AppFixtures.workStartedEvent(),
                            AppFixtures.workNeedsDecisionEvent(
                                id = WorkEventId.parse("event:agent-task:42:needs-decision"),
                            ).copy(evidenceReferences = listOf(publicEvidenceReference())),
                        ),
                    )

                    assertSoftly {
                        state.currentWork.single().status shouldBe
                            MobileStatusPresentation(label = "Needs decision", tone = "Attention")
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
                    val freshWorkItemId = WorkItemId.parse("agent-task:77")
                    val state = MobileOperatorStateContract.fromEvents(
                        listOf(
                            AppFixtures.workStartedEvent(),
                            AppFixtures.workStartedEvent(
                                id = WorkEventId.parse("event:agent-task:77:started"),
                                occurredAt = EventTimestamp.parse("2026-06-02T22:01:00Z"),
                                workItemId = freshWorkItemId,
                                payload = WorkStartedPayload(
                                    title = WorkItemTitle.parse("Refresh operator summary"),
                                    summary = WorkSummary.parse("Agent started a later task."),
                                ),
                            ),
                        ),
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
                        listOf(
                            AppFixtures.workStartedEvent(),
                            AppFixtures.workSucceededEvent(),
                            AppFixtures.workBlockedEvent(
                                id = WorkEventId.parse("event:agent-task:42:blocked-after-success"),
                            ),
                        ),
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
    })

private fun publicEvidenceReference(): EvidenceReference = EvidenceReference(
    kind = EvidenceReferenceKind.CheckRun,
    label = EvidenceLabel.parse("Mobile contract check"),
    target = EvidenceTarget.parse("https://github.com/yonatankarp/agent-desk/actions/runs/26937983933"),
)

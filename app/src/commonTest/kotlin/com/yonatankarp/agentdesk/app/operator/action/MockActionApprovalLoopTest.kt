package com.yonatankarp.agentdesk.app.operator.action

import com.yonatankarp.agentdesk.app.fixtures.AppFixtures
import com.yonatankarp.agentdesk.app.operator.EvidenceLine
import com.yonatankarp.agentdesk.app.operator.OperatorActionIntent
import com.yonatankarp.agentdesk.app.operator.OperatorStateProjector
import com.yonatankarp.agentdesk.core.domain.entities.WorkItem
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemId
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemTitle
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkStatus
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

class MockActionApprovalLoopTest :
    BehaviorSpec({
        given("a mock local resume proposal") {
            val events = listOf(
                AppFixtures.workStartedEvent(),
                AppFixtures.workBlockedEvent(),
            )
            val item = OperatorStateProjector.project(events).workItems.single()
            val proposal = ActionCapabilityPlanner.propose(
                item = item,
                action = OperatorActionIntent.Resume,
                evidenceReferences = listOf(
                    EvidenceLine(
                        kind = "sanitized-note",
                        label = "Resume proposal",
                        target = "mock-action:resume:proposal",
                    ),
                ),
            )
            val loop = MockActionApprovalLoop()

            `when`("the proposal is approved") {
                then("it records actor, rationale, receipt, and a resulting replay event") {
                    val result = loop.decide(
                        proposal = proposal,
                        decision = decision(MockActionDecisionOutcome.Approve),
                        events = events,
                    )

                    assertSoftly(result) {
                        state shouldBe MockActionApprovalState.Approved
                        actor shouldBe "operator:daily-agent"
                        decidedAt shouldBe "2026-06-02T21:22:00Z"
                        rationale shouldBe "Public-safe mock approval."
                        selection shouldBe "approve-resume"
                        sourceWorkItemId shouldBe "agent-task:42"
                        receipt.target.toString() shouldBe "mock-action:resume:approved"
                        resultingEvent?.id.toString() shouldBe "event:agent-task:42:action-resume"
                        replayStateSummary shouldContain "resulting replay event"
                    }
                }
            }

            `when`("the proposal is rejected") {
                then("it records a non-executing rejection receipt") {
                    val result = loop.decide(
                        proposal = proposal,
                        decision = decision(MockActionDecisionOutcome.Reject, selection = "reject-resume"),
                        events = events,
                    )

                    result.state shouldBe MockActionApprovalState.Rejected
                    result.resultingEvent shouldBe null
                    result.receipt.target.toString() shouldBe "mock-action:resume:rejected"
                    result.replayStateSummary shouldContain "no action event was recorded"
                }
            }

            `when`("the proposal is deferred") {
                then("it records a non-executing defer receipt") {
                    val result = loop.decide(
                        proposal = proposal,
                        decision = decision(MockActionDecisionOutcome.Defer, selection = "defer-resume"),
                        events = events,
                    )

                    result.state shouldBe MockActionApprovalState.Deferred
                    result.resultingEvent shouldBe null
                    result.receipt.target.toString() shouldBe "mock-action:resume:deferred"
                }
            }

            `when`("the proposal is canceled") {
                then("it records a non-executing canceled receipt") {
                    val result = loop.decide(
                        proposal = proposal,
                        decision = decision(MockActionDecisionOutcome.Cancel, selection = "cancel-resume"),
                        events = events,
                    )

                    result.state shouldBe MockActionApprovalState.Canceled
                    result.resultingEvent shouldBe null
                    result.receipt.target.toString() shouldBe "mock-action:resume:canceled"
                }
            }
        }

        given("unsupported and failed approvals") {
            `when`("a disabled destructive proposal is approved") {
                then("it records unsupported without executing") {
                    val item = WorkItem(
                        id = WorkItemId.parse("agent-task:42"),
                        title = WorkItemTitle.parse("Review operator action"),
                        status = WorkStatus.Blocked,
                    )
                    val result = MockActionApprovalLoop().decide(
                        proposal = ActionCapabilityPlanner.propose(item, OperatorActionIntent.Stop),
                        decision = decision(MockActionDecisionOutcome.Approve),
                        events = emptyList(),
                    )

                    result.state shouldBe MockActionApprovalState.Unsupported
                    result.resultingEvent shouldBe null
                    result.replayStateSummary shouldContain "Unsupported"
                }
            }

            `when`("a resume approval cannot be applied to the supplied events") {
                then("it records failed without external side effects") {
                    val item = WorkItem(
                        id = WorkItemId.parse("agent-task:99"),
                        title = WorkItemTitle.parse("Missing event history"),
                        status = WorkStatus.Blocked,
                    )
                    val proposal = ActionCapabilityPlanner.propose(item, OperatorActionIntent.Resume)

                    val result = MockActionApprovalLoop().decide(
                        proposal = proposal,
                        decision = decision(MockActionDecisionOutcome.Approve),
                        events = emptyList(),
                    )

                    result.state shouldBe MockActionApprovalState.Failed
                    result.resultingEvent shouldBe null
                    result.replayStateSummary shouldContain "Work item was not found"
                }
            }

            `when`("an approved proposal lacks proposal evidence") {
                then("it records partial success with the local result event") {
                    val events = listOf(
                        AppFixtures.workStartedEvent(),
                        AppFixtures.workBlockedEvent(),
                    )
                    val item = OperatorStateProjector.project(events).workItems.single()

                    val result = MockActionApprovalLoop().decide(
                        proposal = ActionCapabilityPlanner.propose(item, OperatorActionIntent.Resume),
                        decision = decision(MockActionDecisionOutcome.Approve),
                        events = events,
                    )

                    result.state shouldBe MockActionApprovalState.PartialSuccess
                    result.resultingEvent?.id.toString() shouldBe "event:agent-task:42:action-resume"
                    result.replayStateSummary shouldContain "proposal evidence was partial"
                }
            }
        }

        given("decision validation") {
            `when`("decision text is unsafe") {
                then("it rejects without echoing private values") {
                    val unsafeRationale = "Read " + "/" + "home/user/private.log"

                    val error = shouldThrow<IllegalArgumentException> {
                        decision(MockActionDecisionOutcome.Approve, rationale = unsafeRationale)
                    }

                    error.message.orEmpty() shouldContain "Action decision rationale"
                    error.message.orEmpty() shouldNotContain unsafeRationale
                }
            }
        }
    })

private fun decision(
    outcome: MockActionDecisionOutcome,
    rationale: String = "Public-safe mock approval.",
    selection: String = "approve-resume",
): MockActionDecision = MockActionDecision(
    outcome = outcome,
    actor = "operator:daily-agent",
    decidedAt = "2026-06-02T21:22:00Z",
    rationale = rationale,
    selection = selection,
)

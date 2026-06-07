package com.yonatankarp.agentdesk.app.operator.audit

import com.yonatankarp.agentdesk.app.fixtures.AppFixtures
import com.yonatankarp.agentdesk.app.operator.Actor
import com.yonatankarp.agentdesk.app.operator.EvidenceLine
import com.yonatankarp.agentdesk.app.operator.OperatorActionIntent
import com.yonatankarp.agentdesk.app.operator.OperatorStateProjector
import com.yonatankarp.agentdesk.app.operator.action.ActionCapabilityPlanner
import com.yonatankarp.agentdesk.app.operator.action.MockActionApprovalLoop
import com.yonatankarp.agentdesk.app.operator.action.MockActionDecision
import com.yonatankarp.agentdesk.app.operator.action.MockActionDecisionOutcome
import com.yonatankarp.agentdesk.core.domain.events.EventTimestamp
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemId
import com.yonatankarp.agentdesk.testfixtures.workEvents
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain

class AuditTrailProjectorTest :
    BehaviorSpec({
        given("a mock approval result") {
            val events = workEvents {
                started()
                blocked()
            }
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
            val result = MockActionApprovalLoop().decide(
                proposal = proposal,
                decision = decision(),
                events = events,
            )

            `when`("audit entries are projected") {
                then("they distinguish the human decision from the mock agent action") {
                    val entries = AuditTrailProjector.fromMockActionResult(result, recordedAt = auditRecordedAt)

                    entries.map { it.actorKind }.shouldContainExactly(AuditActorKind.Human, AuditActorKind.Agent)
                    entries.map { it.result }.shouldContainExactly(AuditResult.Approved, AuditResult.Approved)
                    entries.map { it.correlationId }.toSet().size shouldBe 1

                    val decisionEntry = entries.first()
                    val actionEntry = entries.last()

                    assertSoftly(decisionEntry) {
                        actor shouldBe Actor.parse("operator:daily-agent")
                        timestamp shouldBe EventTimestamp.parse("2026-06-02T21:22:00Z")
                        recordedAt shouldBe auditRecordedAt
                        action shouldBe "decision.approve-resume"
                        target shouldBe WorkItemId.parse("agent-task:42")
                        sourceItem shouldBe WorkItemId.parse("agent-task:42")
                        evidenceReference.target.toString() shouldBe "mock-action:resume:approved"
                        detail shouldBe "Public-safe mock approval."
                    }

                    assertSoftly(actionEntry) {
                        actor shouldBe Actor.parse("mock-action-adapter")
                        action shouldBe "mock.resume"
                        evidenceReference.target.toString() shouldBe "mock-action:resume"
                        detail shouldContain "resulting replay event"
                    }
                }

                then("a non-executing outcome stamps the action entry with the audit record time, not decision time") {
                    val rejected = MockActionApprovalLoop().decide(
                        proposal = proposal,
                        decision = decision(MockActionDecisionOutcome.Reject),
                        events = events,
                    )

                    val actionEntry = AuditTrailProjector.fromMockActionResult(rejected, recordedAt = auditRecordedAt).last()

                    actionEntry.timestamp shouldBe auditRecordedAt
                    actionEntry.timestamp shouldNotBe rejected.decidedAt
                    actionEntry.recordedAt shouldBe auditRecordedAt
                }

                then("timeline rows expose audit outcomes for detail surfaces") {
                    val lines = AuditTrailProjector.timelineLines(
                        AuditTrailProjector.fromMockActionResult(result, recordedAt = auditRecordedAt),
                    )
                    val text = lines.joinToString("\n") { line ->
                        "${line.timestamp} ${line.actor} ${line.action} ${line.target} ${line.result} ${line.evidence} ${line.detail}"
                    }

                    assertSoftly(text) {
                        shouldContain("human:operator:daily-agent")
                        shouldContain("agent:mock-action-adapter")
                        shouldContain("Approved")
                        shouldContain("sanitized-note Mock resume action -> mock-action:resume")
                    }
                }
            }
        }

        given("an importer event") {
            `when`("it is projected into audit") {
                then("it records a system imported event") {
                    val event = AppFixtures.workBlockedEvent()

                    val entry = AuditTrailProjector.fromImporterEvent(
                        event = event,
                        correlationId = "correlation:import:batch-1",
                        recordedAt = auditRecordedAt,
                    )

                    assertSoftly(entry) {
                        actorKind shouldBe AuditActorKind.System
                        actor shouldBe Actor.parse("mock-adapter")
                        action shouldBe "import.work.blocked"
                        target shouldBe WorkItemId.parse("agent-task:42")
                        result shouldBe AuditResult.Imported
                        correlationId shouldBe "correlation:import:batch-1"
                        detail shouldContain "Imported sanitized replay event"
                    }
                }
            }
        }

        given("a system failure") {
            `when`("it is recorded") {
                then("it remains public-safe and inspectable") {
                    val entry = AuditTrailProjector.systemFailure(
                        id = AuditEntryId.parse("audit:system:import-failed"),
                        actor = Actor.parse("system:importer"),
                        timestamp = EventTimestamp.parse("2026-06-02T21:30:00Z"),
                        recordedAt = auditRecordedAt,
                        action = "import.failed",
                        target = WorkItemId.parse("agent-task:42"),
                        sourceItem = WorkItemId.parse("agent-task:42"),
                        correlationId = "correlation:import:batch-1",
                        detail = "Importer rejected an unsafe sanitized observation.",
                    )

                    assertSoftly(entry) {
                        actorKind shouldBe AuditActorKind.System
                        result shouldBe AuditResult.Failed
                        evidenceReference.target.toString() shouldBe "audit:system:import-failed:evidence"
                    }
                }
            }
        }
    })

private val auditRecordedAt = EventTimestamp.parse("2026-06-02T21:25:00Z")

private fun decision(outcome: MockActionDecisionOutcome = MockActionDecisionOutcome.Approve): MockActionDecision = MockActionDecision(
    outcome = outcome,
    actor = Actor.parse("operator:daily-agent"),
    decidedAt = EventTimestamp.parse("2026-06-02T21:22:00Z"),
    rationale = "Public-safe mock approval.",
    selection = "approve-resume",
)

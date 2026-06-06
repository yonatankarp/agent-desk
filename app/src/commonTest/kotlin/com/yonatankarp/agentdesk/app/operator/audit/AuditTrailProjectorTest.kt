package com.yonatankarp.agentdesk.app.operator.audit

import com.yonatankarp.agentdesk.app.fixtures.AppFixtures
import com.yonatankarp.agentdesk.app.operator.EvidenceLine
import com.yonatankarp.agentdesk.app.operator.OperatorActionIntent
import com.yonatankarp.agentdesk.app.operator.OperatorStateProjector
import com.yonatankarp.agentdesk.app.operator.action.ActionCapabilityPlanner
import com.yonatankarp.agentdesk.app.operator.action.MockActionApprovalLoop
import com.yonatankarp.agentdesk.app.operator.action.MockActionDecision
import com.yonatankarp.agentdesk.app.operator.action.MockActionDecisionOutcome
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

class AuditTrailProjectorTest :
    BehaviorSpec({
        given("a mock approval result") {
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
            val result = MockActionApprovalLoop().decide(
                proposal = proposal,
                decision = decision(),
                events = events,
            )

            `when`("audit entries are projected") {
                then("they distinguish the human decision from the mock agent action") {
                    val entries = AuditTrailProjector.fromMockActionResult(result)

                    entries.map { it.actorKind }.shouldContainExactly(AuditActorKind.Human, AuditActorKind.Agent)
                    entries.map { it.result }.shouldContainExactly(AuditResult.Approved, AuditResult.Approved)
                    entries.map { it.correlationId }.toSet().size shouldBe 1

                    val decisionEntry = entries.first()
                    val actionEntry = entries.last()

                    assertSoftly(decisionEntry) {
                        actor shouldBe "operator:daily-agent"
                        timestamp shouldBe "2026-06-02T21:22:00Z"
                        action shouldBe "decision.approve-resume"
                        target shouldBe "agent-task:42"
                        sourceItem shouldBe "agent-task:42"
                        evidenceReference.target.toString() shouldBe "mock-action:resume:approved"
                        detail shouldBe "Public-safe mock approval."
                    }

                    assertSoftly(actionEntry) {
                        actor shouldBe "mock-action-adapter"
                        action shouldBe "mock.resume"
                        evidenceReference.target.toString() shouldBe "mock-action:resume"
                        detail shouldContain "resulting replay event"
                    }
                }

                then("timeline rows expose audit outcomes for detail surfaces") {
                    val lines = AuditTrailProjector.timelineLines(AuditTrailProjector.fromMockActionResult(result))
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
                    )

                    assertSoftly(entry) {
                        actorKind shouldBe AuditActorKind.System
                        actor shouldBe "mock-adapter"
                        action shouldBe "import.work.blocked"
                        target shouldBe "agent-task:42"
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
                        id = "audit:system:import-failed",
                        actor = "system:importer",
                        timestamp = "2026-06-02T21:30:00Z",
                        action = "import.failed",
                        target = "agent-task:42",
                        sourceItem = "agent-task:42",
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

        given("audit validation") {
            `when`("private details are provided") {
                then("it rejects without echoing the unsafe value") {
                    val unsafeActor = "agent:" + "/" + "home/user/private.log"

                    val error = shouldThrow<IllegalArgumentException> {
                        AuditTrailProjector.systemFailure(
                            id = "audit:system:unsafe",
                            actor = unsafeActor,
                            timestamp = "2026-06-02T21:30:00Z",
                            action = "import.failed",
                            target = "agent-task:42",
                            sourceItem = "agent-task:42",
                            correlationId = "correlation:import:batch-1",
                            detail = "Importer rejected unsafe input.",
                        )
                    }

                    error.message.orEmpty() shouldContain "Audit actor"
                    error.message.orEmpty() shouldNotContain unsafeActor
                }
            }
        }
    })

private fun decision(): MockActionDecision = MockActionDecision(
    outcome = MockActionDecisionOutcome.Approve,
    actor = "operator:daily-agent",
    decidedAt = "2026-06-02T21:22:00Z",
    rationale = "Public-safe mock approval.",
    selection = "approve-resume",
)

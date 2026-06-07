package com.yonatankarp.agentdesk.app.operator.action

import com.yonatankarp.agentdesk.app.fixtures.InMemoryWorkEventRepository
import com.yonatankarp.agentdesk.app.operator.Actor
import com.yonatankarp.agentdesk.app.operator.OperatorActionIntent
import com.yonatankarp.agentdesk.app.operator.audit.AuditActorKind
import com.yonatankarp.agentdesk.app.operator.audit.AuditEntry
import com.yonatankarp.agentdesk.app.operator.audit.AuditResult
import com.yonatankarp.agentdesk.app.operator.audit.AuditTrailRecorder
import com.yonatankarp.agentdesk.app.persistence.AuditRecordReadResult
import com.yonatankarp.agentdesk.app.persistence.AuditRecordRepository
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemId
import com.yonatankarp.agentdesk.testfixtures.eventTimestampAt
import com.yonatankarp.agentdesk.testfixtures.matchers.shouldBePublicSafe
import com.yonatankarp.agentdesk.testfixtures.sanitizedNoteEvidence
import com.yonatankarp.agentdesk.testfixtures.workEvents
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class ActOnWorkItemTest :
    BehaviorSpec({
        given("a resumable work item in the event store") {
            `when`("resume is acted on with explicit approval") {
                then("the gate approves, audit evidence persists, and the event is appended") {
                    val harness = harness()

                    val outcome = harness.act(approved = true)

                    val executed = outcome.shouldBeInstanceOf<ActOutcome.Executed>()
                    assertSoftly {
                        executed.decision.state shouldBe PermissionDecisionState.Approved
                        executed.result.state shouldBe MockActionApprovalState.Approved
                        executed.recordedEvent.id.toString() shouldBe
                            "event:agent-task:42:action-resume:2026-06-02t21:25:00z"
                        harness.eventRepository.readAll().events.last() shouldBe executed.recordedEvent
                        executed.auditEntries shouldHaveSize 3
                        harness.auditRepository.appended.shouldContainExactly(executed.auditEntries)
                        executed.auditEntries.map { it.actorKind }.shouldContainExactly(
                            AuditActorKind.Human,
                            AuditActorKind.Human,
                            AuditActorKind.Agent,
                        )
                    }
                }
            }

            `when`("resume is acted on without approval") {
                then("the gate denies, no event is appended, and the denial is audited") {
                    val harness = harness()
                    val eventCountBefore = harness.eventRepository.readAll().events.size

                    val outcome = harness.act(approved = false)

                    val notExecuted = outcome.shouldBeInstanceOf<ActOutcome.NotExecuted>()
                    assertSoftly {
                        notExecuted.decision.state shouldBe PermissionDecisionState.Denied
                        notExecuted.auditEntries.single().result shouldBe AuditResult.Rejected
                        harness.auditRepository.appended.shouldContainExactly(notExecuted.auditEntries)
                        harness.eventRepository.readAll().events shouldHaveSize eventCountBefore
                        notExecuted.decision.logSummary.shouldBePublicSafe()
                    }
                }
            }

            `when`("a destructive stop is acted on even with approval") {
                then("the gate fails closed and the denial is audited") {
                    val harness = harness()

                    val outcome = harness.act(intent = OperatorActionIntent.Stop, approved = true)

                    val notExecuted = outcome.shouldBeInstanceOf<ActOutcome.NotExecuted>()
                    assertSoftly {
                        notExecuted.decision.state shouldBe PermissionDecisionState.Denied
                        notExecuted.decision.behavior shouldBe PermissionGateBehavior.DenyUnavailable
                        notExecuted.auditEntries.single().result shouldBe AuditResult.Rejected
                        harness.eventRepository.readAll().events shouldHaveSize 2
                    }
                }
            }
        }

        given("a missing work item") {
            `when`("resume is acted on") {
                then("no proposal is gated and no audit record is written") {
                    val harness = harness()

                    val outcome = harness.act(workItemId = WorkItemId.parse("agent-task:99"), approved = true)

                    outcome shouldBe ActOutcome.WorkItemNotFound
                    harness.auditRepository.appended.shouldBeEmpty()
                }
            }
        }

        given("two approved invocations at different instants") {
            `when`("the same item is resumed twice") {
                then("the second act is denied on the now-running item and every audit id stays unique") {
                    val harness = harness()

                    val first = harness.act(approved = true, minute = 25)
                    val second = harness.act(approved = true, minute = 26)

                    first.shouldBeInstanceOf<ActOutcome.Executed>()
                    val denied = second.shouldBeInstanceOf<ActOutcome.NotExecuted>()
                    assertSoftly {
                        denied.decision.state shouldBe PermissionDecisionState.Denied
                        harness.auditRepository.appended shouldHaveSize 4
                        harness.auditRepository.appended.map { it.id }.toSet() shouldHaveSize 4
                    }
                }
            }
        }
    })

private class RecordingAuditRepository : AuditRecordRepository {
    val appended = mutableListOf<AuditEntry>()

    override fun append(entry: AuditEntry) {
        appended += entry
    }

    override fun readAll(): AuditRecordReadResult = AuditRecordReadResult(entries = appended.toList())
}

private class ActHarness(
    val eventRepository: InMemoryWorkEventRepository,
    val auditRepository: RecordingAuditRepository,
    private val useCase: ActOnWorkItem,
) {
    fun act(
        intent: OperatorActionIntent = OperatorActionIntent.Resume,
        workItemId: WorkItemId = WorkItemId.parse("agent-task:42"),
        approved: Boolean,
        minute: Int = 25,
    ): ActOutcome = useCase.act(
        intent = intent,
        workItemId = workItemId,
        actor = Actor.parse("operator:cli"),
        approved = approved,
        now = eventTimestampAt(minute = minute),
    )
}

private fun harness(): ActHarness {
    val eventRepository = InMemoryWorkEventRepository()
    workEvents {
        started()
        blocked(evidence = listOf(sanitizedNoteEvidence("Blocked context", "docs/blocked-context.md")))
    }.forEach(eventRepository::append)
    val auditRepository = RecordingAuditRepository()
    return ActHarness(
        eventRepository = eventRepository,
        auditRepository = auditRepository,
        useCase = ActOnWorkItem(
            eventRepository = eventRepository,
            auditRecorder = AuditTrailRecorder(auditRepository),
        ),
    )
}

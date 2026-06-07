package com.yonatankarp.agentdesk.app.operator.audit

import com.yonatankarp.agentdesk.app.fixtures.projectedWorkItem
import com.yonatankarp.agentdesk.app.operator.Actor
import com.yonatankarp.agentdesk.app.operator.OperatorActionIntent
import com.yonatankarp.agentdesk.app.operator.action.ActionCapabilityPlanner
import com.yonatankarp.agentdesk.app.operator.action.ActionPermissionGate
import com.yonatankarp.agentdesk.app.operator.action.ActionPermissionRequest
import com.yonatankarp.agentdesk.app.operator.action.MockActionApprovalLoop
import com.yonatankarp.agentdesk.app.operator.action.MockActionDecision
import com.yonatankarp.agentdesk.app.operator.action.MockActionDecisionOutcome
import com.yonatankarp.agentdesk.app.operator.action.PermissionedActionClass
import com.yonatankarp.agentdesk.app.persistence.AuditRecordReadResult
import com.yonatankarp.agentdesk.app.persistence.AuditRecordRepository
import com.yonatankarp.agentdesk.testfixtures.eventTimestampAt
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class AuditTrailRecorderTest :
    BehaviorSpec({
        given("a recorder over an audit record repository") {
            `when`("a permission denial is recorded") {
                then("the denial is appended to the trail") {
                    val repository = RecordingAuditRepository()
                    val recorder = AuditTrailRecorder(repository)
                    val denied = ActionPermissionGate.decide(
                        ActionPermissionRequest(
                            proposal = ActionCapabilityPlanner.propose(
                                item = projectedWorkItem {
                                    started()
                                    blocked()
                                },
                                action = OperatorActionIntent.Resume,
                            ),
                            actionClass = PermissionedActionClass.LocalWrite,
                            actor = Actor.parse("operator"),
                            requestedAt = eventTimestampAt(minute = 5),
                            intentSummary = "Resume local mock work.",
                        ),
                    )

                    val entry = recorder.record(denied, recordedAt = eventTimestampAt(minute = 6))

                    repository.appended.shouldContainExactly(entry)
                    entry.result shouldBe AuditResult.Rejected
                }
            }

            `when`("a mock approval outcome is recorded") {
                then("both the decision and action entries are appended in order") {
                    val repository = RecordingAuditRepository()
                    val recorder = AuditTrailRecorder(repository)
                    val item = projectedWorkItem {
                        started()
                        blocked()
                    }
                    val result = MockActionApprovalLoop().decide(
                        proposal = ActionCapabilityPlanner.propose(item, OperatorActionIntent.Resume),
                        decision = MockActionDecision(
                            outcome = MockActionDecisionOutcome.Defer,
                            actor = Actor.parse("operator:daily-agent"),
                            decidedAt = eventTimestampAt(minute = 7),
                            rationale = "Deferring until evidence is reviewed.",
                            selection = "defer-resume",
                        ),
                        events = emptyList(),
                    )

                    val entries = recorder.record(result, recordedAt = eventTimestampAt(minute = 8))

                    repository.appended.shouldContainExactly(entries)
                    repository.appended.map { it.actorKind }.shouldContainExactly(
                        AuditActorKind.Human,
                        AuditActorKind.Agent,
                    )
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

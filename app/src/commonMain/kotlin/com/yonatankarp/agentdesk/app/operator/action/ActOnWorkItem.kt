package com.yonatankarp.agentdesk.app.operator.action

import com.yonatankarp.agentdesk.app.operator.Actor
import com.yonatankarp.agentdesk.app.operator.EvidenceLine
import com.yonatankarp.agentdesk.app.operator.OperatorActionIntent
import com.yonatankarp.agentdesk.app.operator.OperatorStateProjector
import com.yonatankarp.agentdesk.app.operator.audit.AuditEntry
import com.yonatankarp.agentdesk.app.operator.audit.AuditTrailRecorder
import com.yonatankarp.agentdesk.app.persistence.WorkEventRepository
import com.yonatankarp.agentdesk.core.domain.events.EventTimestamp
import com.yonatankarp.agentdesk.core.domain.events.WorkEvent
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemId

/**
 * Routes an operator action through the documented preview/decide/record loop:
 * plan a proposal, gate it, run the approval loop on approval, and persist every
 * decision — including denials — through the durable audit recorder. The work
 * event is appended only when the loop actually executed.
 */
class ActOnWorkItem(
    private val eventRepository: WorkEventRepository,
    private val auditRecorder: AuditTrailRecorder,
    private val approvalLoop: MockActionApprovalLoop = MockActionApprovalLoop(),
) {
    fun act(
        intent: OperatorActionIntent,
        workItemId: WorkItemId,
        actor: Actor,
        approved: Boolean,
        now: EventTimestamp,
    ): ActOutcome {
        val events = eventRepository.readAll().events
        val item = OperatorStateProjector.project(events).workItems.firstOrNull { it.id == workItemId }
            ?: return ActOutcome.WorkItemNotFound

        // The proposal carries the public-safe evidence already attached to the
        // item's replay events — the material the operator reviewed before acting.
        val evidence = events
            .filter { it.workItemId == workItemId }
            .flatMap { event -> event.evidenceReferences }
            .map { reference ->
                EvidenceLine(
                    kind = reference.kind.wireName,
                    label = reference.label.toString(),
                    target = reference.target.toString(),
                )
            }
        val proposal = ActionCapabilityPlanner.propose(item, intent, evidenceReferences = evidence)
        val approval = if (approved) {
            ActionPermissionApproval(
                outcome = PermissionApprovalOutcome.Approve,
                actor = actor,
                decidedAt = now,
                rationale = APPROVAL_RATIONALE,
            )
        } else {
            null
        }
        val decision = ActionPermissionGate.decide(
            ActionPermissionRequest(
                proposal = proposal,
                actionClass = proposal.riskClass.toPermissionedActionClass(),
                actor = actor,
                requestedAt = now,
                intentSummary = "Operator requested ${intent.wireName} via the act command.",
                approval = approval,
            ),
        )
        val decisionEntry = auditRecorder.record(decision, recordedAt = now)
        if (decision.state != PermissionDecisionState.Approved) {
            return ActOutcome.NotExecuted(
                proposal = proposal,
                decision = decision,
                approvalState = null,
                auditEntries = listOf(decisionEntry),
            )
        }

        val result = approvalLoop.decide(
            proposal = proposal,
            decision = MockActionDecision(
                outcome = MockActionDecisionOutcome.Approve,
                actor = actor,
                decidedAt = now,
                rationale = APPROVAL_RATIONALE,
                selection = "approve-${intent.wireName}",
            ),
            events = events,
        )
        val auditEntries = listOf(decisionEntry) + auditRecorder.record(result, recordedAt = now)
        val recordedEvent = result.resultingEvent
            ?: return ActOutcome.NotExecuted(
                proposal = proposal,
                decision = decision,
                approvalState = result.state,
                auditEntries = auditEntries,
            )

        eventRepository.append(recordedEvent)
        return ActOutcome.Executed(
            proposal = proposal,
            decision = decision,
            result = result,
            recordedEvent = recordedEvent,
            auditEntries = auditEntries,
        )
    }

    private fun ActionRiskClass.toPermissionedActionClass(): PermissionedActionClass = when (this) {
        ActionRiskClass.ReadOnly -> PermissionedActionClass.ReadOnly
        ActionRiskClass.LocalPreview, ActionRiskClass.LocalMutation -> PermissionedActionClass.LocalWrite
        ActionRiskClass.ExternalSideEffect -> PermissionedActionClass.ExternalSend
        ActionRiskClass.Destructive -> PermissionedActionClass.Destructive
    }

    companion object {
        private const val APPROVAL_RATIONALE = "Operator approved this action explicitly on the act command."
    }
}

sealed interface ActOutcome {
    /** Pre-gate input miss: no proposal was built and no audit record is written. */
    data object WorkItemNotFound : ActOutcome

    /** The gate denied, or the approval loop finished without executing; audit evidence was written. */
    data class NotExecuted(
        val proposal: ActionProposal,
        val decision: ActionPermissionDecision,
        val approvalState: MockActionApprovalState?,
        val auditEntries: List<AuditEntry>,
    ) : ActOutcome

    /** The action executed; the resulting event and audit evidence were persisted. */
    data class Executed(
        val proposal: ActionProposal,
        val decision: ActionPermissionDecision,
        val result: MockActionApprovalResult,
        val recordedEvent: WorkEvent,
        val auditEntries: List<AuditEntry>,
    ) : ActOutcome
}

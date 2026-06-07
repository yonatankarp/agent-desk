package com.yonatankarp.agentdesk.app.operator.action

import com.yonatankarp.agentdesk.app.operator.Actor
import com.yonatankarp.agentdesk.app.operator.MockOperatorActionAdapter
import com.yonatankarp.agentdesk.app.operator.OperatorActionException
import com.yonatankarp.agentdesk.app.operator.OperatorActionIntent
import com.yonatankarp.agentdesk.core.domain.events.EventTimestamp
import com.yonatankarp.agentdesk.core.domain.events.EvidenceLabel
import com.yonatankarp.agentdesk.core.domain.events.EvidenceReference
import com.yonatankarp.agentdesk.core.domain.events.EvidenceReferenceKind
import com.yonatankarp.agentdesk.core.domain.events.EvidenceTarget
import com.yonatankarp.agentdesk.core.domain.events.WorkEvent
import com.yonatankarp.agentdesk.core.domain.valueobjects.PublicSafeTextPolicy
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemId

data class MockActionDecision(
    val outcome: MockActionDecisionOutcome,
    val actor: Actor,
    val decidedAt: EventTimestamp,
    val rationale: String,
    val selection: String,
) {
    init {
        require(rationale.isNotBlank()) { "Action decision rationale must not be blank" }
        require(selection.isNotBlank()) { "Action decision selection must not be blank" }
        PublicSafeTextPolicy.requirePublicSafe(rationale, fieldName = "Action decision rationale")
        PublicSafeTextPolicy.requirePublicSafe(selection, fieldName = "Action decision selection")
    }
}

enum class MockActionDecisionOutcome {
    Approve,
    Reject,
    Defer,
    Cancel,
}

data class MockActionApprovalResult(
    val state: MockActionApprovalState,
    val actor: Actor,
    val decidedAt: EventTimestamp,
    val rationale: String,
    val selection: String,
    val sourceWorkItemId: WorkItemId,
    val action: OperatorActionIntent,
    val receipt: EvidenceReference,
    val resultingEvent: WorkEvent?,
    val replayStateSummary: String,
) {
    init {
        PublicSafeTextPolicy.requirePublicSafe(replayStateSummary, fieldName = "Action approval replay state")
    }
}

enum class MockActionApprovalState {
    Approved,
    Rejected,
    Deferred,
    Canceled,
    Failed,
    PartialSuccess,
    Unsupported,
}

class MockActionApprovalLoop(
    private val adapter: MockOperatorActionAdapter = MockOperatorActionAdapter(),
) {
    fun decide(
        proposal: ActionProposal,
        decision: MockActionDecision,
        events: List<WorkEvent>,
    ): MockActionApprovalResult = when (decision.outcome) {
        MockActionDecisionOutcome.Approve -> approve(proposal, decision, events)

        MockActionDecisionOutcome.Reject -> nonExecutingResult(
            proposal = proposal,
            decision = decision,
            state = MockActionApprovalState.Rejected,
            summary = "Rejected by operator; no action event was recorded.",
        )

        MockActionDecisionOutcome.Defer -> nonExecutingResult(
            proposal = proposal,
            decision = decision,
            state = MockActionApprovalState.Deferred,
            summary = "Deferred by operator; no action event was recorded.",
        )

        MockActionDecisionOutcome.Cancel -> nonExecutingResult(
            proposal = proposal,
            decision = decision,
            state = MockActionApprovalState.Canceled,
            summary = "Canceled by operator; no action event was recorded.",
        )
    }

    private fun approve(
        proposal: ActionProposal,
        decision: MockActionDecision,
        events: List<WorkEvent>,
    ): MockActionApprovalResult {
        if (proposal.executionMode != ActionExecutionMode.ProposalOnly || proposal.capability.state.isDisabled) {
            return nonExecutingResult(
                proposal = proposal,
                decision = decision,
                state = MockActionApprovalState.Unsupported,
                summary = "Unsupported approval; no action event was recorded.",
            )
        }
        if (proposal.action != OperatorActionIntent.Resume) {
            return nonExecutingResult(
                proposal = proposal,
                decision = decision,
                state = MockActionApprovalState.Unsupported,
                summary = "Unsupported action intent; no action event was recorded.",
            )
        }

        return try {
            val event = adapter.perform(
                intent = proposal.action,
                workItemId = proposal.target.workItemId,
                events = events,
            )
            val state = if (proposal.evidenceReferences.isEmpty()) {
                MockActionApprovalState.PartialSuccess
            } else {
                MockActionApprovalState.Approved
            }
            MockActionApprovalResult(
                state = state,
                actor = decision.actor,
                decidedAt = decision.decidedAt,
                rationale = decision.rationale,
                selection = decision.selection,
                sourceWorkItemId = proposal.target.workItemId,
                action = proposal.action,
                receipt = receiptFor(proposal, state),
                resultingEvent = event,
                replayStateSummary = if (state == MockActionApprovalState.PartialSuccess) {
                    "Recorded mock resume action; proposal evidence was partial."
                } else {
                    "Recorded mock resume action and resulting replay event."
                },
            )
        } catch (exception: OperatorActionException) {
            nonExecutingResult(
                proposal = proposal,
                decision = decision,
                state = MockActionApprovalState.Failed,
                summary = exception.message ?: "Mock action approval failed.",
            )
        }
    }

    private fun nonExecutingResult(
        proposal: ActionProposal,
        decision: MockActionDecision,
        state: MockActionApprovalState,
        summary: String,
    ): MockActionApprovalResult = MockActionApprovalResult(
        state = state,
        actor = decision.actor,
        decidedAt = decision.decidedAt,
        rationale = decision.rationale,
        selection = decision.selection,
        sourceWorkItemId = proposal.target.workItemId,
        action = proposal.action,
        receipt = receiptFor(proposal, state),
        resultingEvent = null,
        replayStateSummary = summary,
    )

    private fun receiptFor(
        proposal: ActionProposal,
        state: MockActionApprovalState,
    ): EvidenceReference = EvidenceReference(
        kind = EvidenceReferenceKind.SanitizedNote,
        label = EvidenceLabel.parse("Mock action ${state.name}"),
        target = EvidenceTarget.parse("mock-action:${proposal.action.wireName}:${state.name.lowercase()}"),
    )
}

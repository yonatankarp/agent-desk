package com.yonatankarp.agentdesk.app.operator.action

import com.yonatankarp.agentdesk.core.domain.events.EventTimestamp
import com.yonatankarp.agentdesk.core.domain.events.EvidenceLabel
import com.yonatankarp.agentdesk.core.domain.events.EvidenceReference
import com.yonatankarp.agentdesk.core.domain.events.EvidenceReferenceKind
import com.yonatankarp.agentdesk.core.domain.events.EvidenceTarget
import com.yonatankarp.agentdesk.core.domain.valueobjects.PublicSafeTextPolicy

data class ActionPermissionRequest(
    val proposal: ActionProposal,
    val actionClass: PermissionedActionClass,
    val actor: String,
    val requestedAt: String,
    val intentSummary: String,
    val ambiguous: Boolean = false,
    val approval: ActionPermissionApproval? = null,
) {
    init {
        EventTimestamp.parse(requestedAt)
        require(actor.isNotBlank()) { "Permission actor must not be blank." }
        require(intentSummary.isNotBlank()) { "Permission intent summary must not be blank." }
        PublicSafeTextPolicy.requirePublicSafe(actor, fieldName = "Permission actor")
        PublicSafeTextPolicy.requirePublicSafe(intentSummary, fieldName = "Permission intent summary")
    }
}

data class ActionPermissionApproval(
    val outcome: PermissionApprovalOutcome,
    val actor: String,
    val decidedAt: String,
    val rationale: String,
) {
    init {
        EventTimestamp.parse(decidedAt)
        require(actor.isNotBlank()) { "Permission approval actor must not be blank." }
        require(rationale.isNotBlank()) { "Permission approval rationale must not be blank." }
        PublicSafeTextPolicy.requirePublicSafe(actor, fieldName = "Permission approval actor")
        PublicSafeTextPolicy.requirePublicSafe(rationale, fieldName = "Permission approval rationale")
    }
}

data class ActionPermissionDecision(
    val state: PermissionDecisionState,
    val actionClass: PermissionedActionClass,
    val behavior: PermissionGateBehavior,
    val actor: String,
    val decidedAt: String,
    val action: String,
    val target: String,
    val receipt: EvidenceReference,
    val logSummary: String,
) {
    init {
        EventTimestamp.parse(decidedAt)
        require(action.isNotBlank()) { "Permission decision action must not be blank." }
        require(target.isNotBlank()) { "Permission decision target must not be blank." }
        require(logSummary.isNotBlank()) { "Permission decision summary must not be blank." }
        PublicSafeTextPolicy.requirePublicSafe(actor, fieldName = "Permission decision actor")
        PublicSafeTextPolicy.requirePublicSafe(action, fieldName = "Permission decision action")
        PublicSafeTextPolicy.requirePublicSafe(target, fieldName = "Permission decision target")
        PublicSafeTextPolicy.requirePublicSafe(logSummary, fieldName = "Permission decision summary")
    }
}

enum class PermissionedActionClass {
    ReadOnly,
    LocalWrite,
    ExternalSend,
    PublicPost,
    Destructive,
    AccountSecurity,
    PurchasePayment,
    Credential,
}

enum class PermissionGateBehavior {
    AllowWithoutApproval,
    RequireLocalConfirmation,
    RequireExplicitApproval,
    DenyUnavailable,
    RequireClarification,
}

enum class PermissionApprovalOutcome {
    Approve,
    Reject,
    Cancel,
}

enum class PermissionDecisionState {
    Approved,
    Denied,
    Canceled,
    RequiresClarification,
    Unsupported,
}

object ActionPermissionGate {
    fun behaviorFor(actionClass: PermissionedActionClass): PermissionGateBehavior = when (actionClass) {
        PermissionedActionClass.ReadOnly -> PermissionGateBehavior.AllowWithoutApproval

        PermissionedActionClass.LocalWrite -> PermissionGateBehavior.RequireLocalConfirmation

        PermissionedActionClass.ExternalSend,
        PermissionedActionClass.PublicPost,
        PermissionedActionClass.Destructive,
        PermissionedActionClass.AccountSecurity,
        PermissionedActionClass.PurchasePayment,
        PermissionedActionClass.Credential,
        -> PermissionGateBehavior.RequireExplicitApproval
    }

    fun decide(request: ActionPermissionRequest): ActionPermissionDecision {
        if (request.ambiguous) {
            return decision(
                request = request,
                state = PermissionDecisionState.RequiresClarification,
                behavior = PermissionGateBehavior.RequireClarification,
                summary = "Permission request requires clarification; no action is allowed.",
            )
        }

        if (request.proposal.capability.state == ActionCapabilityState.Unsupported) {
            return decision(
                request = request,
                state = PermissionDecisionState.Unsupported,
                behavior = PermissionGateBehavior.DenyUnavailable,
                summary = "Permission request is unsupported by the current adapter; no action is allowed.",
            )
        }

        if (request.proposal.capability.state.isDisabled) {
            return decision(
                request = request,
                state = PermissionDecisionState.Denied,
                behavior = PermissionGateBehavior.DenyUnavailable,
                summary = "Permission request is unavailable in the current proposal-only model; no action is allowed.",
            )
        }

        val behavior = behaviorFor(request.actionClass)
        if (behavior == PermissionGateBehavior.AllowWithoutApproval) {
            return decision(
                request = request,
                state = PermissionDecisionState.Approved,
                behavior = behavior,
                summary = "Read-only permission allowed without external side effects.",
            )
        }

        val approval = request.approval
        if (approval == null) {
            return decision(
                request = request,
                state = PermissionDecisionState.Denied,
                behavior = behavior,
                summary = "Permission approval is required before this action can proceed.",
            )
        }

        return when (approval.outcome) {
            PermissionApprovalOutcome.Approve -> decision(
                request = request,
                state = PermissionDecisionState.Approved,
                behavior = behavior,
                actor = approval.actor,
                decidedAt = approval.decidedAt,
                summary = approvalSummary(request.actionClass, behavior),
            )

            PermissionApprovalOutcome.Reject -> decision(
                request = request,
                state = PermissionDecisionState.Denied,
                behavior = behavior,
                actor = approval.actor,
                decidedAt = approval.decidedAt,
                summary = "Permission rejected by operator; no action is allowed.",
            )

            PermissionApprovalOutcome.Cancel -> decision(
                request = request,
                state = PermissionDecisionState.Canceled,
                behavior = behavior,
                actor = approval.actor,
                decidedAt = approval.decidedAt,
                summary = "Permission canceled by operator; no action is allowed.",
            )
        }
    }

    private fun approvalSummary(
        actionClass: PermissionedActionClass,
        behavior: PermissionGateBehavior,
    ): String = when (behavior) {
        PermissionGateBehavior.RequireLocalConfirmation ->
            "Local permission approved; action remains limited to the local proposal loop."

        PermissionGateBehavior.RequireExplicitApproval ->
            "${actionClass.name} permission approved for planning only; no external executor is attached."

        PermissionGateBehavior.AllowWithoutApproval,
        PermissionGateBehavior.DenyUnavailable,
        PermissionGateBehavior.RequireClarification,
        -> "Permission decision recorded."
    }

    private fun decision(
        request: ActionPermissionRequest,
        state: PermissionDecisionState,
        behavior: PermissionGateBehavior,
        actor: String = request.actor,
        decidedAt: String = request.requestedAt,
        summary: String,
    ): ActionPermissionDecision = ActionPermissionDecision(
        state = state,
        actionClass = request.actionClass,
        behavior = behavior,
        actor = actor,
        decidedAt = decidedAt,
        action = request.proposal.action.wireName,
        target = request.proposal.target.workItemId,
        receipt = EvidenceReference(
            kind = EvidenceReferenceKind.SanitizedNote,
            label = EvidenceLabel.parse("Permission ${state.name}"),
            target = EvidenceTarget.parse(
                "permission:${request.proposal.action.wireName}:${request.actionClass.name.lowercase()}:${state.name.lowercase()}",
            ),
        ),
        logSummary = summary,
    )
}

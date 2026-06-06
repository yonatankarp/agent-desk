package com.yonatankarp.agentdesk.app.operator.action

import com.yonatankarp.agentdesk.app.operator.EvidenceLine
import com.yonatankarp.agentdesk.app.operator.OperatorActionIntent
import com.yonatankarp.agentdesk.app.operator.OperatorStatePresenter
import com.yonatankarp.agentdesk.core.domain.entities.WorkItem
import com.yonatankarp.agentdesk.core.domain.valueobjects.PublicSafeTextPolicy
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkStatus

data class ActionProposal(
    val target: ActionTarget,
    val action: OperatorActionIntent,
    val capability: ActionCapability,
    val expectedEffect: String,
    val riskClass: ActionRiskClass,
    val evidenceReferences: List<EvidenceLine>,
    val requiredConfirmation: ActionConfirmationRequirement,
    val executionMode: ActionExecutionMode = ActionExecutionMode.ProposalOnly,
) {
    init {
        require(expectedEffect.isNotBlank()) { "Action proposal expected effect must not be blank" }
        PublicSafeTextPolicy.requirePublicSafe(expectedEffect, fieldName = "Action proposal expected effect")
        require(executionMode == ActionExecutionMode.ProposalOnly) {
            "Action proposals must not include an executor."
        }
        require(capability.state.isDisabled || capability.disabledReason == null) {
            "Enabled action capabilities must not include a disabled reason."
        }
        require(!capability.state.isDisabled || !capability.disabledReason.isNullOrBlank()) {
            "Disabled action capabilities must explain why the action is unavailable."
        }
        require(riskClass != ActionRiskClass.Destructive || requiredConfirmation == ActionConfirmationRequirement.ExplicitApproval) {
            "Destructive action proposals require explicit approval."
        }
        require(
            riskClass != ActionRiskClass.ExternalSideEffect ||
                requiredConfirmation != ActionConfirmationRequirement.None,
        ) {
            "External side-effect proposals require confirmation."
        }
    }
}

data class ActionTarget(
    val workItemId: String,
    val title: String,
    val status: String,
) {
    init {
        require(workItemId.isNotBlank()) { "Action target work item id must not be blank" }
        require(title.isNotBlank()) { "Action target title must not be blank" }
        require(status.isNotBlank()) { "Action target status must not be blank" }
        PublicSafeTextPolicy.requirePublicSafe(workItemId, fieldName = "Action target work item id")
        PublicSafeTextPolicy.requirePublicSafe(title, fieldName = "Action target title")
        PublicSafeTextPolicy.requirePublicSafe(status, fieldName = "Action target status")
    }
}

data class ActionCapability(
    val state: ActionCapabilityState,
    val disabledReason: String?,
) {
    init {
        disabledReason?.let {
            require(it.isNotBlank()) { "Disabled action reason must not be blank" }
            PublicSafeTextPolicy.requirePublicSafe(it, fieldName = "Disabled action reason")
        }
    }
}

enum class ActionCapabilityState(
    val isDisabled: Boolean,
) {
    PreviewOnly(isDisabled = false),
    RequiresConfirmation(isDisabled = false),
    ExternalSideEffectUnavailable(isDisabled = true),
    DestructiveUnavailable(isDisabled = true),
    Unsupported(isDisabled = true),
    ReadOnlyUnavailable(isDisabled = true),
}

enum class ActionRiskClass {
    ReadOnly,
    LocalPreview,
    LocalMutation,
    ExternalSideEffect,
    Destructive,
}

enum class ActionConfirmationRequirement {
    None,
    ConfirmLocal,
    ExplicitApproval,
}

enum class ActionExecutionMode {
    ProposalOnly,
}

object ActionCapabilityPlanner {
    fun propose(
        item: WorkItem,
        action: OperatorActionIntent,
        evidenceReferences: List<EvidenceLine> = emptyList(),
    ): ActionProposal {
        val target = item.toActionTarget()
        return when (action) {
            OperatorActionIntent.Inspect ->
                ActionProposal(
                    target = target,
                    action = action,
                    capability = ActionCapability(
                        state = ActionCapabilityState.PreviewOnly,
                        disabledReason = null,
                    ),
                    expectedEffect = "Open the public-safe evidence detail for this work item.",
                    riskClass = ActionRiskClass.ReadOnly,
                    evidenceReferences = evidenceReferences,
                    requiredConfirmation = ActionConfirmationRequirement.None,
                )

            OperatorActionIntent.Resume ->
                resumeProposal(
                    target = target,
                    action = action,
                    status = item.status,
                    evidenceReferences = evidenceReferences,
                )

            OperatorActionIntent.Stop ->
                ActionProposal(
                    target = target,
                    action = action,
                    capability = ActionCapability(
                        state = ActionCapabilityState.DestructiveUnavailable,
                        disabledReason = "Real stop/delete/provider-control actions are unavailable in this read-only proposal model.",
                    ),
                    expectedEffect = "No action is executed; this only records why a destructive control is unavailable.",
                    riskClass = ActionRiskClass.Destructive,
                    evidenceReferences = evidenceReferences,
                    requiredConfirmation = ActionConfirmationRequirement.ExplicitApproval,
                )
        }
    }

    fun unsupportedExternalProposal(
        target: ActionTarget,
        action: OperatorActionIntent,
        expectedEffect: String,
        evidenceReferences: List<EvidenceLine> = emptyList(),
    ): ActionProposal = ActionProposal(
        target = target,
        action = action,
        capability = ActionCapability(
            state = ActionCapabilityState.ExternalSideEffectUnavailable,
            disabledReason = "External sends, posts, purchases, account changes, and provider writes are unavailable.",
        ),
        expectedEffect = expectedEffect,
        riskClass = ActionRiskClass.ExternalSideEffect,
        evidenceReferences = evidenceReferences,
        requiredConfirmation = ActionConfirmationRequirement.ExplicitApproval,
    )

    fun unsupportedProposal(
        target: ActionTarget,
        action: OperatorActionIntent,
        expectedEffect: String = "No action is executed; this action is unsupported by the current adapter.",
        evidenceReferences: List<EvidenceLine> = emptyList(),
    ): ActionProposal = ActionProposal(
        target = target,
        action = action,
        capability = ActionCapability(
            state = ActionCapabilityState.Unsupported,
            disabledReason = "The current adapter does not support this operator action.",
        ),
        expectedEffect = expectedEffect,
        riskClass = ActionRiskClass.LocalPreview,
        evidenceReferences = evidenceReferences,
        requiredConfirmation = ActionConfirmationRequirement.ConfirmLocal,
    )

    private fun resumeProposal(
        target: ActionTarget,
        action: OperatorActionIntent,
        status: WorkStatus,
        evidenceReferences: List<EvidenceLine>,
    ): ActionProposal {
        if (status !in resumableStatuses) {
            return ActionProposal(
                target = target,
                action = action,
                capability = ActionCapability(
                    state = ActionCapabilityState.ReadOnlyUnavailable,
                    disabledReason = "Resume is unavailable for this status in the read-only proposal model.",
                ),
                expectedEffect = "No action is executed; the work item is not in a resumable state.",
                riskClass = ActionRiskClass.LocalPreview,
                evidenceReferences = evidenceReferences,
                requiredConfirmation = ActionConfirmationRequirement.ConfirmLocal,
            )
        }

        return ActionProposal(
            target = target,
            action = action,
            capability = ActionCapability(
                state = ActionCapabilityState.RequiresConfirmation,
                disabledReason = null,
            ),
            expectedEffect = "Preview a mock/local resume request without touching external providers.",
            riskClass = ActionRiskClass.LocalPreview,
            evidenceReferences = evidenceReferences,
            requiredConfirmation = ActionConfirmationRequirement.ConfirmLocal,
        )
    }

    private fun WorkItem.toActionTarget(): ActionTarget = ActionTarget(
        workItemId = id.toString(),
        title = title.toString(),
        status = OperatorStatePresenter.presentationFor(status).label,
    )

    private val resumableStatuses = setOf(WorkStatus.NeedsDecision, WorkStatus.Blocked, WorkStatus.Waiting)
}

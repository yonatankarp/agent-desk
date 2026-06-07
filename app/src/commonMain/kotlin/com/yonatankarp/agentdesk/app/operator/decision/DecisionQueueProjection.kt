package com.yonatankarp.agentdesk.app.operator.decision

import com.yonatankarp.agentdesk.app.operator.EvidenceLine
import com.yonatankarp.agentdesk.app.operator.OperatorState
import com.yonatankarp.agentdesk.app.operator.OperatorStatePresenter
import com.yonatankarp.agentdesk.core.domain.events.EventSource
import com.yonatankarp.agentdesk.core.domain.events.EventTimestamp
import com.yonatankarp.agentdesk.core.domain.events.WorkEvent
import com.yonatankarp.agentdesk.core.domain.events.WorkNeedsDecisionPayload
import com.yonatankarp.agentdesk.core.domain.valueobjects.PublicSafeTextPolicy
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemId
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkStatus

data class DecisionQueueProjection(
    val items: List<DecisionQueueItem>,
    val states: List<DecisionQueueItemState>,
)

data class DecisionQueueItem(
    val id: String,
    val workItemId: WorkItemId,
    val requestedAt: EventTimestamp,
    val updatedAt: EventTimestamp,
    val status: String,
    val state: DecisionQueueItemState,
    val request: DecisionRequest,
    val unavailableReason: String,
)

data class DecisionRequest(
    val prompt: String,
    val options: List<DecisionOption>,
    val recommendedOptionId: String?,
    val consequences: String,
    val risk: DecisionRisk,
    val evidenceReferences: List<EvidenceLine>,
    val source: EventSource,
    val expiresAt: EventTimestamp?,
    val urgency: DecisionUrgency,
) {
    init {
        require(prompt.isNotBlank()) { "Decision prompt must not be blank" }
        require(options.isNotEmpty()) { "Decision request must include at least one option" }
        require(options.map { it.id }.toSet().size == options.size) {
            "Decision request options must have unique ids"
        }
        require(recommendedOptionId == null || options.any { it.id == recommendedOptionId }) {
            "Recommended decision option must reference a provided option"
        }
        PublicSafeTextPolicy.requirePublicSafe(prompt, fieldName = "Decision prompt")
        PublicSafeTextPolicy.requirePublicSafe(consequences, fieldName = "Decision consequences")
    }
}

data class DecisionOption(
    val id: String,
    val label: String,
    val consequence: String,
) {
    init {
        require(id.matches(WorkItemId.validPattern)) {
            "Decision option id must be lowercase letters, numbers, '.', '_', ':', or '-'"
        }
        require(label.isNotBlank()) { "Decision option label must not be blank" }
        require(consequence.isNotBlank()) { "Decision option consequence must not be blank" }
        PublicSafeTextPolicy.requirePublicSafe(id, fieldName = "Decision option id")
        PublicSafeTextPolicy.requirePublicSafe(label, fieldName = "Decision option label")
        PublicSafeTextPolicy.requirePublicSafe(consequence, fieldName = "Decision option consequence")
    }
}

enum class DecisionQueueItemState {
    Pending,
    Approved,
    Rejected,
    Deferred,
    Expired,
    Superseded,
    InsufficientEvidence,
}

enum class DecisionRisk {
    ReadOnly,
    LocalOnly,
    ExternalSideEffect,
}

enum class DecisionUrgency {
    Normal,
    Stale,
    Expired,
}

object DecisionQueueProjector {
    fun project(
        state: OperatorState,
        now: EventTimestamp? = null,
    ): DecisionQueueProjection {
        val workItemsById = state.workItems.associateBy { it.id }
        val staleWorkItemIds = state.staleAttention.mapTo(mutableSetOf()) { it.workItemId }
        val latestEventAtByWorkItem = state.events
            .groupBy { it.workItemId }
            .mapValues { (_, events) -> events.maxOf { it.occurredAt } }
        val items = state.events
            .filter { it.payload is WorkNeedsDecisionPayload }
            .map { event ->
                val workItemId = event.workItemId
                val workItem = workItemsById[workItemId]
                val status = workItem?.status
                val evidence = event.evidenceReferences.map { reference ->
                    EvidenceLine(
                        kind = reference.kind.wireName,
                        label = reference.label.toString(),
                        target = reference.target.toString(),
                    )
                }
                val request = event.toDecisionRequest(
                    evidenceReferences = evidence,
                    isStale = workItemId in staleWorkItemIds,
                )
                val itemState = request.toQueueState(
                    status = status,
                    now = now,
                )

                DecisionQueueItem(
                    id = "decision:${event.id}",
                    workItemId = workItemId,
                    requestedAt = event.occurredAt,
                    updatedAt = latestEventAtByWorkItem[workItemId] ?: event.occurredAt,
                    status = status?.let(OperatorStatePresenter::presentationFor)?.label ?: "Read-only",
                    state = itemState,
                    request = request,
                    unavailableReason = itemState.unavailableReason(),
                )
            }

        return DecisionQueueProjection(
            items = items,
            states = items.mapTo(linkedSetOf()) { it.state }.toList(),
        )
    }

    private fun WorkEvent.toDecisionRequest(
        evidenceReferences: List<EvidenceLine>,
        isStale: Boolean,
    ): DecisionRequest {
        val prompt = (payload as WorkNeedsDecisionPayload).reason.toString()
        return DecisionRequest(
            prompt = prompt,
            options = listOf(
                DecisionOption(
                    id = "inspect-evidence",
                    label = "Inspect evidence",
                    consequence = "Operator reviews the public-safe evidence before deciding.",
                ),
                DecisionOption(
                    id = "defer",
                    label = "Defer",
                    consequence = "Decision remains unresolved and no action is executed.",
                ),
                DecisionOption(
                    id = "mark-insufficient-evidence",
                    label = "Insufficient evidence",
                    consequence = "Decision stays read-only until more evidence is available.",
                ),
            ),
            recommendedOptionId = if (evidenceReferences.isEmpty()) {
                "mark-insufficient-evidence"
            } else {
                "inspect-evidence"
            },
            consequences = "This projection records decision semantics only; it does not execute actions or write to providers.",
            risk = DecisionRisk.ReadOnly,
            evidenceReferences = evidenceReferences,
            source = source,
            expiresAt = null,
            urgency = if (isStale) DecisionUrgency.Stale else DecisionUrgency.Normal,
        )
    }

    private fun DecisionRequest.toQueueState(
        status: WorkStatus?,
        now: EventTimestamp?,
    ): DecisionQueueItemState = when {
        expiresAt != null && now != null && expiresAt <= now -> DecisionQueueItemState.Expired
        status == WorkStatus.NeedsDecision && evidenceReferences.isEmpty() -> DecisionQueueItemState.InsufficientEvidence
        status == WorkStatus.NeedsDecision -> DecisionQueueItemState.Pending
        else -> DecisionQueueItemState.Superseded
    }

    private fun DecisionQueueItemState.unavailableReason(): String = when (this) {
        DecisionQueueItemState.Pending ->
            "Read-only projection: operator decisions are visible, but action execution is not wired in this slice."

        DecisionQueueItemState.InsufficientEvidence ->
            "Read-only projection: more public-safe evidence is required before this decision can be resolved."

        DecisionQueueItemState.Expired ->
            "Read-only projection: this decision request is expired and cannot execute an action."

        DecisionQueueItemState.Superseded ->
            "Read-only projection: later replay state superseded this decision request."

        DecisionQueueItemState.Approved,
        DecisionQueueItemState.Rejected,
        DecisionQueueItemState.Deferred,
        ->
            "Read-only projection: recorded outcomes are inspectable, but provider writes are unavailable."
    }
}

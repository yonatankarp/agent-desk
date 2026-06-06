package com.yonatankarp.agentdesk.app.operator.audit

import com.yonatankarp.agentdesk.app.operator.action.ActionPermissionDecision
import com.yonatankarp.agentdesk.app.operator.action.MockActionApprovalResult
import com.yonatankarp.agentdesk.app.operator.action.MockActionApprovalState
import com.yonatankarp.agentdesk.app.operator.action.PermissionDecisionState
import com.yonatankarp.agentdesk.core.domain.events.EventTimestamp
import com.yonatankarp.agentdesk.core.domain.events.EvidenceLabel
import com.yonatankarp.agentdesk.core.domain.events.EvidenceReference
import com.yonatankarp.agentdesk.core.domain.events.EvidenceReferenceKind
import com.yonatankarp.agentdesk.core.domain.events.EvidenceTarget
import com.yonatankarp.agentdesk.core.domain.events.WorkEvent
import com.yonatankarp.agentdesk.core.domain.valueobjects.PublicSafeTextPolicy

data class AuditEntry(
    val id: String,
    val actor: String,
    val actorKind: AuditActorKind,
    val timestamp: String,
    val action: String,
    val target: String,
    val result: AuditResult,
    val sourceItem: String,
    val correlationId: String,
    val evidenceReference: EvidenceReference,
    val detail: String,
) {
    init {
        EventTimestamp.parse(timestamp)
        require(id.isNotBlank()) { "Audit entry id must not be blank" }
        require(actor.isNotBlank()) { "Audit actor must not be blank" }
        require(action.isNotBlank()) { "Audit action must not be blank" }
        require(target.isNotBlank()) { "Audit target must not be blank" }
        require(sourceItem.isNotBlank()) { "Audit source item must not be blank" }
        require(correlationId.isNotBlank()) { "Audit correlation id must not be blank" }
        require(detail.isNotBlank()) { "Audit detail must not be blank" }
        PublicSafeTextPolicy.requirePublicSafe(id, fieldName = "Audit entry id")
        PublicSafeTextPolicy.requirePublicSafe(actor, fieldName = "Audit actor")
        PublicSafeTextPolicy.requirePublicSafe(action, fieldName = "Audit action")
        PublicSafeTextPolicy.requirePublicSafe(target, fieldName = "Audit target")
        PublicSafeTextPolicy.requirePublicSafe(sourceItem, fieldName = "Audit source item")
        PublicSafeTextPolicy.requirePublicSafe(correlationId, fieldName = "Audit correlation id")
        PublicSafeTextPolicy.requirePublicSafe(detail, fieldName = "Audit detail")
    }
}

data class AuditTimelineLine(
    val timestamp: String,
    val actor: String,
    val action: String,
    val target: String,
    val result: String,
    val evidence: String,
    val detail: String,
)

enum class AuditActorKind {
    Human,
    Agent,
    System,
}

enum class AuditResult {
    Approved,
    Rejected,
    Deferred,
    Canceled,
    Failed,
    PartialSuccess,
    Unsupported,
    Imported,
}

object AuditTrailProjector {
    fun fromMockActionResult(result: MockActionApprovalResult): List<AuditEntry> {
        val decisionEntry = AuditEntry(
            id = "audit:${result.sourceWorkItemId}:${result.action.wireName}:decision:${result.decidedAt}",
            actor = result.actor,
            actorKind = AuditActorKind.Human,
            timestamp = result.decidedAt,
            action = "decision.${result.selection}",
            target = result.sourceWorkItemId,
            result = result.state.toAuditResult(),
            sourceItem = result.sourceWorkItemId,
            correlationId = "correlation:${result.sourceWorkItemId}:${result.action.wireName}:${result.decidedAt}",
            evidenceReference = result.receipt,
            detail = result.rationale,
        )

        val actionEntry = AuditEntry(
            id = "audit:${result.sourceWorkItemId}:${result.action.wireName}:action:${result.decidedAt}",
            actor = result.resultingEvent?.source?.toString() ?: "mock-action-adapter",
            actorKind = AuditActorKind.Agent,
            timestamp = result.resultingEvent?.occurredAt?.toString() ?: result.decidedAt,
            action = "mock.${result.action.wireName}",
            target = result.sourceWorkItemId,
            result = result.state.toAuditResult(),
            sourceItem = result.sourceWorkItemId,
            correlationId = decisionEntry.correlationId,
            evidenceReference = result.resultingEvent?.evidenceReferences?.firstOrNull() ?: result.receipt,
            detail = result.replayStateSummary,
        )

        return listOf(decisionEntry, actionEntry)
    }

    fun fromPermissionDecision(decision: ActionPermissionDecision): AuditEntry = AuditEntry(
        id = "audit:${decision.target}:${decision.action}:permission:${decision.decidedAt}",
        actor = decision.actor,
        actorKind = AuditActorKind.Human,
        timestamp = decision.decidedAt,
        action = "permission.${decision.actionClass.name.lowercase()}",
        target = decision.target,
        result = decision.state.toAuditResult(),
        sourceItem = decision.target,
        correlationId = "correlation:${decision.target}:${decision.action}:permission:${decision.decidedAt}",
        evidenceReference = decision.receipt,
        detail = decision.logSummary,
    )

    fun fromImporterEvent(
        event: WorkEvent,
        correlationId: String,
    ): AuditEntry = AuditEntry(
        id = "audit:${event.id}",
        actor = event.source.toString(),
        actorKind = AuditActorKind.System,
        timestamp = event.occurredAt.toString(),
        action = "import.${event.type.wireName}",
        target = event.workItemId.toString(),
        result = AuditResult.Imported,
        sourceItem = event.workItemId.toString(),
        correlationId = correlationId,
        evidenceReference = event.evidenceReferences.firstOrNull()
            ?: sanitizedNote(
                label = "Imported replay event",
                target = "audit:${event.id}",
            ),
        detail = "Imported sanitized replay event ${event.type.wireName}.",
    )

    fun systemFailure(
        id: String,
        actor: String,
        timestamp: String,
        action: String,
        target: String,
        sourceItem: String,
        correlationId: String,
        detail: String,
    ): AuditEntry = AuditEntry(
        id = id,
        actor = actor,
        actorKind = AuditActorKind.System,
        timestamp = timestamp,
        action = action,
        target = target,
        result = AuditResult.Failed,
        sourceItem = sourceItem,
        correlationId = correlationId,
        evidenceReference = sanitizedNote(
            label = "System failure",
            target = "$id:evidence",
        ),
        detail = detail,
    )

    fun timelineLines(entries: List<AuditEntry>): List<AuditTimelineLine> = entries.map { entry ->
        AuditTimelineLine(
            timestamp = entry.timestamp,
            actor = "${entry.actorKind.name.lowercase()}:${entry.actor}",
            action = entry.action,
            target = entry.target,
            result = entry.result.name,
            evidence = "${entry.evidenceReference.kind.wireName} ${entry.evidenceReference.label} -> ${entry.evidenceReference.target}",
            detail = entry.detail,
        )
    }

    private fun MockActionApprovalState.toAuditResult(): AuditResult = when (this) {
        MockActionApprovalState.Approved -> AuditResult.Approved
        MockActionApprovalState.Rejected -> AuditResult.Rejected
        MockActionApprovalState.Deferred -> AuditResult.Deferred
        MockActionApprovalState.Canceled -> AuditResult.Canceled
        MockActionApprovalState.Failed -> AuditResult.Failed
        MockActionApprovalState.PartialSuccess -> AuditResult.PartialSuccess
        MockActionApprovalState.Unsupported -> AuditResult.Unsupported
    }

    private fun PermissionDecisionState.toAuditResult(): AuditResult = when (this) {
        PermissionDecisionState.Approved -> AuditResult.Approved
        PermissionDecisionState.Denied -> AuditResult.Rejected
        PermissionDecisionState.Canceled -> AuditResult.Canceled
        PermissionDecisionState.RequiresClarification -> AuditResult.Deferred
        PermissionDecisionState.Unsupported -> AuditResult.Unsupported
    }

    private fun sanitizedNote(
        label: String,
        target: String,
    ): EvidenceReference = EvidenceReference(
        kind = EvidenceReferenceKind.SanitizedNote,
        label = EvidenceLabel.parse(label),
        target = EvidenceTarget.parse(target),
    )
}

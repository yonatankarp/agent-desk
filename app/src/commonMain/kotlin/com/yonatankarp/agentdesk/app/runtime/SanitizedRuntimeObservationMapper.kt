package com.yonatankarp.agentdesk.app.runtime

import com.yonatankarp.agentdesk.core.domain.events.EventSource
import com.yonatankarp.agentdesk.core.domain.events.EventTimestamp
import com.yonatankarp.agentdesk.core.domain.events.EvidenceLabel
import com.yonatankarp.agentdesk.core.domain.events.EvidenceReference
import com.yonatankarp.agentdesk.core.domain.events.EvidenceReferenceKind
import com.yonatankarp.agentdesk.core.domain.events.EvidenceTarget
import com.yonatankarp.agentdesk.core.domain.events.ProvenanceId
import com.yonatankarp.agentdesk.core.domain.events.WorkBlockedPayload
import com.yonatankarp.agentdesk.core.domain.events.WorkCanceledPayload
import com.yonatankarp.agentdesk.core.domain.events.WorkEvent
import com.yonatankarp.agentdesk.core.domain.events.WorkEventId
import com.yonatankarp.agentdesk.core.domain.events.WorkFailedPayload
import com.yonatankarp.agentdesk.core.domain.events.WorkNeedsDecisionPayload
import com.yonatankarp.agentdesk.core.domain.events.WorkProvenance
import com.yonatankarp.agentdesk.core.domain.events.WorkStartedPayload
import com.yonatankarp.agentdesk.core.domain.events.WorkSucceededPayload
import com.yonatankarp.agentdesk.core.domain.valueobjects.PublicSafeTextPolicy
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemId
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemTitle
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkSummary

class SanitizedRuntimeObservationMapper {
    fun toWorkEvent(observation: RuntimeWorkObservation): WorkEvent {
        observation.requirePublicSafe()

        return WorkEvent(
            id = WorkEventId.parse(observation.eventId),
            occurredAt = EventTimestamp.parse(observation.occurredAt),
            source = EventSource.parse(observation.source),
            workItemId = WorkItemId.parse(observation.workItemId),
            payload = observation.toPayload(),
            evidenceReferences = observation.evidenceReferences.map { it.toDomain() },
            provenance = observation.provenance?.toDomain(),
        )
    }

    private fun RuntimeWorkObservation.toPayload() = when (kind) {
        RuntimeWorkObservationKind.Started ->
            WorkStartedPayload(
                title = WorkItemTitle.parse(requireNotNull(title) { "Started observations require a title" }),
                summary = summary?.let(WorkSummary::parse),
            )

        RuntimeWorkObservationKind.NeedsDecision ->
            WorkNeedsDecisionPayload(
                reason = WorkSummary.parse(requireNotNull(reason) { "NeedsDecision observations require a reason" }),
            )

        RuntimeWorkObservationKind.Blocked ->
            WorkBlockedPayload(
                reason = WorkSummary.parse(requireNotNull(reason) { "Blocked observations require a reason" }),
            )

        RuntimeWorkObservationKind.Succeeded -> WorkSucceededPayload

        RuntimeWorkObservationKind.Failed ->
            WorkFailedPayload(
                reason = WorkSummary.parse(requireNotNull(reason) { "Failed observations require a reason" }),
            )

        RuntimeWorkObservationKind.Canceled ->
            WorkCanceledPayload(
                reason = reason?.let(WorkSummary::parse),
            )
    }

    private fun RuntimeWorkObservation.requirePublicSafe() {
        listOf(
            "eventId" to eventId,
            "occurredAt" to occurredAt,
            "source" to source,
            "workItemId" to workItemId,
            "title" to title,
            "summary" to summary,
            "reason" to reason,
        ).forEach { (label, value) ->
            value?.requirePublicSafeRuntimeField(label)
        }
        evidenceReferences.forEachIndexed { index, reference ->
            reference.kind.requirePublicSafeRuntimeField("evidenceReferences[$index].kind")
            reference.label.requirePublicSafeRuntimeField("evidenceReferences[$index].label")
            reference.target.requirePublicSafeRuntimeField("evidenceReferences[$index].target")
        }
        provenance?.fields()?.forEach { (label, value) ->
            value?.requirePublicSafeRuntimeField("provenance.$label")
        }
    }

    private fun RuntimeEvidenceReference.toDomain(): EvidenceReference = EvidenceReference(
        kind = EvidenceReferenceKind.fromWireName(kind),
        label = EvidenceLabel.parse(label),
        target = EvidenceTarget.parse(target),
    )

    private fun RuntimeWorkProvenance.toDomain(): WorkProvenance = WorkProvenance(
        projectId = projectId?.let(ProvenanceId::parse),
        workspaceId = workspaceId?.let(ProvenanceId::parse),
        sourceId = sourceId?.let(ProvenanceId::parse),
        ownerId = ownerId?.let(ProvenanceId::parse),
        agentId = agentId?.let(ProvenanceId::parse),
        modelId = modelId?.let(ProvenanceId::parse),
        toolId = toolId?.let(ProvenanceId::parse),
        runId = runId?.let(ProvenanceId::parse),
        objectiveId = objectiveId?.let(ProvenanceId::parse),
        parentHandoffId = parentHandoffId?.let(ProvenanceId::parse),
        archiveRecordId = archiveRecordId?.let(ProvenanceId::parse),
    )

    private fun RuntimeWorkProvenance.fields(): List<Pair<String, String?>> = listOf(
        "projectId" to projectId,
        "workspaceId" to workspaceId,
        "sourceId" to sourceId,
        "ownerId" to ownerId,
        "agentId" to agentId,
        "modelId" to modelId,
        "toolId" to toolId,
        "runId" to runId,
        "objectiveId" to objectiveId,
        "parentHandoffId" to parentHandoffId,
        "archiveRecordId" to archiveRecordId,
    )

    private fun String.requirePublicSafeRuntimeField(label: String) {
        val fieldName = "Runtime observation $label"
        PublicSafeTextPolicy.normalizeAndRequirePublicSafe(this, fieldName = fieldName, maxLength = 512)
    }
}

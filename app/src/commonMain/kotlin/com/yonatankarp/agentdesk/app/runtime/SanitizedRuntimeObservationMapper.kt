package com.yonatankarp.agentdesk.app.runtime

import com.yonatankarp.agentdesk.core.domain.events.EventSource
import com.yonatankarp.agentdesk.core.domain.events.EventTimestamp
import com.yonatankarp.agentdesk.core.domain.events.EvidenceLabel
import com.yonatankarp.agentdesk.core.domain.events.EvidenceReference
import com.yonatankarp.agentdesk.core.domain.events.EvidenceReferenceKind
import com.yonatankarp.agentdesk.core.domain.events.EvidenceTarget
import com.yonatankarp.agentdesk.core.domain.events.WorkBlockedPayload
import com.yonatankarp.agentdesk.core.domain.events.WorkCanceledPayload
import com.yonatankarp.agentdesk.core.domain.events.WorkEvent
import com.yonatankarp.agentdesk.core.domain.events.WorkEventId
import com.yonatankarp.agentdesk.core.domain.events.WorkFailedPayload
import com.yonatankarp.agentdesk.core.domain.events.WorkNeedsDecisionPayload
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
    }

    private fun RuntimeEvidenceReference.toDomain(): EvidenceReference = EvidenceReference(
        kind = EvidenceReferenceKind.fromWireName(kind),
        label = EvidenceLabel.parse(label),
        target = EvidenceTarget.parse(target),
    )

    private fun String.requirePublicSafeRuntimeField(label: String) {
        val fieldName = "Runtime observation $label"
        PublicSafeTextPolicy.normalizeAndRequirePublicSafe(this, fieldName = fieldName, maxLength = 512)
    }
}

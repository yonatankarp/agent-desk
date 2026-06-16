package com.yonatankarp.agentdesk.app.serialization

import com.yonatankarp.agentdesk.core.domain.events.EventSource
import com.yonatankarp.agentdesk.core.domain.events.EventTimestamp
import com.yonatankarp.agentdesk.core.domain.events.ProvenanceId
import com.yonatankarp.agentdesk.core.domain.events.WorkBlockedPayload
import com.yonatankarp.agentdesk.core.domain.events.WorkCanceledPayload
import com.yonatankarp.agentdesk.core.domain.events.WorkEvent
import com.yonatankarp.agentdesk.core.domain.events.WorkEventId
import com.yonatankarp.agentdesk.core.domain.events.WorkEventPayload
import com.yonatankarp.agentdesk.core.domain.events.WorkEventType
import com.yonatankarp.agentdesk.core.domain.events.WorkFailedPayload
import com.yonatankarp.agentdesk.core.domain.events.WorkNeedsDecisionPayload
import com.yonatankarp.agentdesk.core.domain.events.WorkProvenance
import com.yonatankarp.agentdesk.core.domain.events.WorkStartedPayload
import com.yonatankarp.agentdesk.core.domain.events.WorkSucceededPayload
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemId
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemTitle
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkSummary
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object WorkEventJson {
    private val json =
        Json {
            encodeDefaults = false
            explicitNulls = false
            ignoreUnknownKeys = false
        }

    fun encode(event: WorkEvent): String = json.encodeToString(toRecord(event))

    fun decode(raw: String): WorkEvent = json.decodeFromString<WorkEventRecord>(raw).toDomain()

    fun toRecord(event: WorkEvent): WorkEventRecord = WorkEventRecord(
        id = event.id.toString(),
        occurredAt = event.occurredAt.toString(),
        source = event.source.toString(),
        workItemId = event.workItemId.toString(),
        type = event.type.wireName,
        payload = event.payload.toRecord(),
        evidenceReferences = event.evidenceReferences.map { it.toRecord() },
        provenance = event.provenance?.toRecord(),
    )

    fun fromRecord(record: WorkEventRecord): WorkEvent = record.toDomain()

    private fun WorkEventRecord.toDomain(): WorkEvent = WorkEvent(
        id = WorkEventId.parse(id),
        occurredAt = EventTimestamp.parse(occurredAt),
        source = EventSource.parse(source),
        workItemId = WorkItemId.parse(workItemId),
        payload = payload.toDomainPayload(type.toEventType()),
        evidenceReferences = evidenceReferences.map { it.toDomain() },
        provenance = provenance?.toDomain(),
    )

    private fun WorkProvenance.toRecord(): WorkProvenanceRecord = WorkProvenanceRecord(
        projectId = projectId?.toString(),
        workspaceId = workspaceId?.toString(),
        sourceId = sourceId?.toString(),
        ownerId = ownerId?.toString(),
        agentId = agentId?.toString(),
        modelId = modelId?.toString(),
        toolId = toolId?.toString(),
        runId = runId?.toString(),
        objectiveId = objectiveId?.toString(),
        parentHandoffId = parentHandoffId?.toString(),
        archiveRecordId = archiveRecordId?.toString(),
    )

    private fun WorkProvenanceRecord.toDomain(): WorkProvenance = WorkProvenance(
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

    private fun WorkEventPayload.toRecord(): WorkEventPayloadRecord = when (this) {
        is WorkStartedPayload ->
            WorkEventPayloadRecord(
                title = title.toString(),
                summary = summary?.toString(),
            )

        is WorkNeedsDecisionPayload -> WorkEventPayloadRecord(reason = reason.toString())

        is WorkBlockedPayload -> WorkEventPayloadRecord(reason = reason.toString())

        WorkSucceededPayload -> WorkEventPayloadRecord()

        is WorkFailedPayload -> WorkEventPayloadRecord(reason = reason.toString())

        is WorkCanceledPayload -> WorkEventPayloadRecord(reason = reason?.toString())
    }

    private fun WorkEventPayloadRecord.toDomainPayload(type: WorkEventType): WorkEventPayload = when (type) {
        WorkEventType.WorkStarted ->
            WorkStartedPayload(
                title = WorkItemTitle.parse(requireNotNull(title) { "work.started payload requires title" }),
                summary = summary?.let(WorkSummary::parse),
            )

        WorkEventType.WorkNeedsDecision ->
            WorkNeedsDecisionPayload(
                reason = WorkSummary.parse(requireNotNull(reason) { "work.needs-decision payload requires reason" }),
            )

        WorkEventType.WorkBlocked ->
            WorkBlockedPayload(
                reason = WorkSummary.parse(requireNotNull(reason) { "work.blocked payload requires reason" }),
            )

        WorkEventType.WorkSucceeded -> WorkSucceededPayload

        WorkEventType.WorkFailed ->
            WorkFailedPayload(
                reason = WorkSummary.parse(requireNotNull(reason) { "work.failed payload requires reason" }),
            )

        WorkEventType.WorkCanceled ->
            WorkCanceledPayload(
                reason = reason?.let(WorkSummary::parse),
            )
    }

    private fun String.toEventType(): WorkEventType = WorkEventType.entries.firstOrNull { it.wireName == this }
        ?: throw IllegalArgumentException("Unknown work event type")
}

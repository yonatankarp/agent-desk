package com.yonatankarp.agentdesk.app.serialization

import com.yonatankarp.agentdesk.core.domain.events.EventSource
import com.yonatankarp.agentdesk.core.domain.events.EventTimestamp
import com.yonatankarp.agentdesk.core.domain.events.ProvenanceId
import com.yonatankarp.agentdesk.core.domain.events.RecordedContentDigest
import com.yonatankarp.agentdesk.core.domain.events.RecordedDigestAlgorithm
import com.yonatankarp.agentdesk.core.domain.events.RecordedVerificationInputBinding
import com.yonatankarp.agentdesk.core.domain.events.RecordedVerificationKind
import com.yonatankarp.agentdesk.core.domain.events.RecordedVerificationResult
import com.yonatankarp.agentdesk.core.domain.events.RecordedVerificationState
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
import com.yonatankarp.agentdesk.core.domain.events.WorkVerificationOutcome
import com.yonatankarp.agentdesk.core.domain.events.WorkVerificationRecordedPayload
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

        is WorkVerificationRecordedPayload ->
            WorkEventPayloadRecord(
                outcome = outcome.wireName(),
                verificationAttempted = verificationAttempted,
                knownFailures = knownFailures.map { it.toString() },
                touchedArtifacts = touchedArtifacts.map { it.toString() },
                residualRisks = residualRisks.map { it.toString() },
                verificationResults = results.map { it.toRecord() },
            )
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

        WorkEventType.WorkVerificationRecorded ->
            WorkVerificationRecordedPayload(
                outcome = requireNotNull(outcome) { "work.verification-recorded payload requires outcome" }
                    .toVerificationOutcome(),
                verificationAttempted = requireNotNull(verificationAttempted) {
                    "work.verification-recorded payload requires verificationAttempted"
                },
                knownFailures = knownFailures.map(WorkSummary::parse),
                touchedArtifacts = touchedArtifacts.map(WorkSummary::parse),
                residualRisks = residualRisks.map(WorkSummary::parse),
                results = verificationResults.map { it.toDomain() },
            )
    }

    private fun String.toEventType(): WorkEventType = WorkEventType.entries.firstOrNull { it.wireName == this }
        ?: throw IllegalArgumentException("Unknown work event type")

    private fun RecordedVerificationResult.toRecord(): VerificationResultRecord = VerificationResultRecord(
        name = name.toString(),
        kind = kind.wireName(),
        result = result.wireName(),
        durationMillis = durationMillis,
        outputReference = outputReference.toString(),
        failureSummary = failureSummary?.toString(),
        evidenceReference = evidenceReference.toRecord(),
        inputBinding = inputBinding?.toRecord(),
    )

    private fun VerificationResultRecord.toDomain(): RecordedVerificationResult = RecordedVerificationResult(
        name = WorkSummary.parse(name),
        kind = kind.toVerificationKind(),
        result = result.toVerificationState(),
        durationMillis = durationMillis,
        outputReference = WorkSummary.parse(outputReference),
        failureSummary = failureSummary?.let(WorkSummary::parse),
        evidenceReference = evidenceReference.toDomain(),
        inputBinding = inputBinding?.toDomain(),
    )

    private fun RecordedVerificationInputBinding.toRecord(): VerificationInputBindingRecord = VerificationInputBindingRecord(
        digest = digest.toString(),
        algorithm = algorithm.wireName(),
        capturedAt = capturedAt.toString(),
    )

    private fun VerificationInputBindingRecord.toDomain(): RecordedVerificationInputBinding = RecordedVerificationInputBinding(
        digest = RecordedContentDigest.parseSha256(digest),
        algorithm = algorithm.toDigestAlgorithm(),
        capturedAt = EventTimestamp.parse(capturedAt),
    )

    private fun WorkVerificationOutcome.wireName(): String = when (this) {
        WorkVerificationOutcome.Ready -> "ready"
        WorkVerificationOutcome.NotReady -> "not-ready"
        WorkVerificationOutcome.Blocked -> "blocked"
        WorkVerificationOutcome.Unknown -> "unknown"
    }

    private fun String.toVerificationOutcome(): WorkVerificationOutcome = when (this) {
        "ready" -> WorkVerificationOutcome.Ready
        "not-ready" -> WorkVerificationOutcome.NotReady
        "blocked" -> WorkVerificationOutcome.Blocked
        "unknown" -> WorkVerificationOutcome.Unknown
        else -> throw IllegalArgumentException("Unknown verification outcome")
    }

    private fun RecordedVerificationKind.wireName(): String = when (this) {
        RecordedVerificationKind.LocalTest -> "local-test"
        RecordedVerificationKind.CiCheck -> "ci-check"
        RecordedVerificationKind.SmokeRun -> "smoke-run"
        RecordedVerificationKind.ManualQa -> "manual-qa"
    }

    private fun String.toVerificationKind(): RecordedVerificationKind = when (this) {
        "local-test" -> RecordedVerificationKind.LocalTest
        "ci-check" -> RecordedVerificationKind.CiCheck
        "smoke-run" -> RecordedVerificationKind.SmokeRun
        "manual-qa" -> RecordedVerificationKind.ManualQa
        else -> throw IllegalArgumentException("Unknown verification result kind")
    }

    private fun RecordedVerificationState.wireName(): String = when (this) {
        RecordedVerificationState.Passed -> "passed"
        RecordedVerificationState.Failed -> "failed"
        RecordedVerificationState.Skipped -> "skipped"
        RecordedVerificationState.Unknown -> "unknown"
    }

    private fun String.toVerificationState(): RecordedVerificationState = when (this) {
        "passed" -> RecordedVerificationState.Passed
        "failed" -> RecordedVerificationState.Failed
        "skipped" -> RecordedVerificationState.Skipped
        "unknown" -> RecordedVerificationState.Unknown
        else -> throw IllegalArgumentException("Unknown verification result state")
    }

    private fun RecordedDigestAlgorithm.wireName(): String = when (this) {
        RecordedDigestAlgorithm.Sha256 -> "sha-256"
    }

    private fun String.toDigestAlgorithm(): RecordedDigestAlgorithm = when (this) {
        "sha-256" -> RecordedDigestAlgorithm.Sha256
        else -> throw IllegalArgumentException("Unknown verification digest algorithm")
    }
}

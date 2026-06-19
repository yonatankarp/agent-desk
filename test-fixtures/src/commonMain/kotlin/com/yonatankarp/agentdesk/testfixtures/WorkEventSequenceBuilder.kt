package com.yonatankarp.agentdesk.testfixtures

import com.yonatankarp.agentdesk.core.domain.events.EventTimestamp
import com.yonatankarp.agentdesk.core.domain.events.EvidenceReference
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
import com.yonatankarp.agentdesk.core.domain.events.WorkFailedPayload
import com.yonatankarp.agentdesk.core.domain.events.WorkNeedsDecisionPayload
import com.yonatankarp.agentdesk.core.domain.events.WorkProvenance
import com.yonatankarp.agentdesk.core.domain.events.WorkStartedPayload
import com.yonatankarp.agentdesk.core.domain.events.WorkVerificationOutcome
import com.yonatankarp.agentdesk.core.domain.events.WorkVerificationRecordedPayload
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemId
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemTitle
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkSummary

/** Builds an ordered list of public-safe work events with canonical defaults. */
fun workEvents(
    fixtures: CanonicalWorkEventFixtures = WorkEventFixtures,
    block: WorkEventSequenceBuilder.() -> Unit,
): List<WorkEvent> = WorkEventSequenceBuilder(fixtures).apply(block).build()

class WorkEventSequenceBuilder internal constructor(
    private val fixtures: CanonicalWorkEventFixtures,
) {
    private val events = mutableListOf<WorkEvent>()

    fun started(
        workItemId: String? = null,
        at: EventTimestamp = fixtures.startedAt,
        title: String? = null,
        summary: String? = null,
        evidence: List<EvidenceReference> = emptyList(),
        provenance: WorkProvenance? = null,
    ) {
        val itemId = itemId(workItemId)
        events += fixtures.workStartedEvent(
            id = eventId(itemId, suffix = "started"),
            occurredAt = at,
            workItemId = itemId,
            payload = WorkStartedPayload(
                title = title?.let(WorkItemTitle::parse) ?: fixtures.workTitle,
                summary = summary?.let(WorkSummary::parse) ?: fixtures.startedSummary,
            ),
        ).withEvidence(evidence).withProvenance(provenance)
    }

    fun blocked(
        workItemId: String? = null,
        at: EventTimestamp = fixtures.blockedAt,
        reason: String? = null,
        evidence: List<EvidenceReference> = emptyList(),
        provenance: WorkProvenance? = null,
    ) {
        val itemId = itemId(workItemId)
        events += fixtures.workBlockedEvent(
            id = eventId(itemId, suffix = "blocked"),
            occurredAt = at,
            workItemId = itemId,
            payload = WorkBlockedPayload(
                reason = reason?.let(WorkSummary::parse) ?: fixtures.blockedReason,
            ),
        ).withEvidence(evidence).withProvenance(provenance)
    }

    fun needsDecision(
        workItemId: String? = null,
        at: EventTimestamp = fixtures.needsDecisionAt,
        reason: String? = null,
        evidence: List<EvidenceReference> = emptyList(),
        provenance: WorkProvenance? = null,
    ) {
        val itemId = itemId(workItemId)
        val id = eventId(itemId, suffix = "needs-decision")
        val event = when (reason) {
            null -> fixtures.workNeedsDecisionEvent(id = id, occurredAt = at, workItemId = itemId)

            else -> fixtures.workNeedsDecisionEvent(
                id = id,
                occurredAt = at,
                workItemId = itemId,
                payload = WorkNeedsDecisionPayload(reason = WorkSummary.parse(reason)),
            )
        }
        events += event.withEvidence(evidence).withProvenance(provenance)
    }

    fun succeeded(
        workItemId: String? = null,
        at: EventTimestamp = fixtures.terminalAt,
        evidence: List<EvidenceReference> = emptyList(),
        provenance: WorkProvenance? = null,
    ) {
        val itemId = itemId(workItemId)
        events += fixtures.workSucceededEvent(
            id = eventId(itemId, suffix = "succeeded"),
            occurredAt = at,
            workItemId = itemId,
        ).withEvidence(evidence).withProvenance(provenance)
    }

    fun failed(
        workItemId: String? = null,
        at: EventTimestamp = fixtures.terminalAt,
        reason: String? = null,
        evidence: List<EvidenceReference> = emptyList(),
        provenance: WorkProvenance? = null,
    ) {
        val itemId = itemId(workItemId)
        val id = eventId(itemId, suffix = "failed")
        val event = when (reason) {
            null -> fixtures.workFailedEvent(id = id, occurredAt = at, workItemId = itemId)

            else -> fixtures.workFailedEvent(
                id = id,
                occurredAt = at,
                workItemId = itemId,
                payload = WorkFailedPayload(reason = WorkSummary.parse(reason)),
            )
        }
        events += event.withEvidence(evidence).withProvenance(provenance)
    }

    fun canceled(
        workItemId: String? = null,
        at: EventTimestamp = fixtures.terminalAt,
        reason: String? = null,
        evidence: List<EvidenceReference> = emptyList(),
        provenance: WorkProvenance? = null,
    ) {
        val itemId = itemId(workItemId)
        val id = eventId(itemId, suffix = "canceled")
        val event = when (reason) {
            null -> fixtures.workCanceledEvent(id = id, occurredAt = at, workItemId = itemId)

            else -> fixtures.workCanceledEvent(
                id = id,
                occurredAt = at,
                workItemId = itemId,
                payload = WorkCanceledPayload(reason = WorkSummary.parse(reason)),
            )
        }
        events += event.withEvidence(evidence).withProvenance(provenance)
    }

    fun verificationRecorded(
        workItemId: String? = null,
        at: EventTimestamp = fixtures.terminalAt,
        outcome: WorkVerificationOutcome = WorkVerificationOutcome.Ready,
        verificationAttempted: Boolean = true,
        knownFailures: List<String> = emptyList(),
        touchedArtifacts: List<String> = listOf("app/src/commonMain/kotlin/com/yonatankarp/agentdesk/app/operator/verification/VerificationEvidence.kt"),
        residualRisks: List<String> = emptyList(),
        results: List<RecordedVerificationResult> = listOf(defaultVerificationResult(capturedAt = at)),
        evidence: List<EvidenceReference> = emptyList(),
        provenance: WorkProvenance? = null,
    ) {
        val itemId = itemId(workItemId)
        events += WorkEvent(
            id = eventId(itemId, suffix = "verification-recorded"),
            occurredAt = at,
            source = fixtures.eventSource,
            workItemId = itemId,
            payload = WorkVerificationRecordedPayload(
                outcome = outcome,
                verificationAttempted = verificationAttempted,
                knownFailures = knownFailures.map(WorkSummary::parse),
                touchedArtifacts = touchedArtifacts.map(WorkSummary::parse),
                residualRisks = residualRisks.map(WorkSummary::parse),
                results = results,
            ),
        ).withEvidence(evidence).withProvenance(provenance)
    }

    /** Escape hatch for events the named builders cannot express. */
    fun event(event: WorkEvent) {
        events += event
    }

    internal fun build(): List<WorkEvent> = events.toList()

    private fun itemId(raw: String?): WorkItemId = raw?.let(WorkItemId::parse) ?: fixtures.workItemId

    private fun eventId(
        itemId: WorkItemId,
        suffix: String,
    ): WorkEventId = WorkEventId.parse("event:${itemId.value}:$suffix")

    private fun WorkEvent.withEvidence(evidence: List<EvidenceReference>): WorkEvent = if (evidence.isEmpty()) this else copy(evidenceReferences = evidence)

    private fun WorkEvent.withProvenance(provenance: WorkProvenance?): WorkEvent = provenance?.let { copy(provenance = it) } ?: this
}

fun recordedVerificationResult(
    name: String = "Gradle check",
    kind: RecordedVerificationKind = RecordedVerificationKind.LocalTest,
    result: RecordedVerificationState = RecordedVerificationState.Passed,
    durationMillis: Long? = 1200,
    outputReference: String = "checks/gradle-check",
    failureSummary: String? = null,
    evidenceReference: EvidenceReference = checkRunEvidence(
        "Gradle check",
        "https://github.com/yonatankarp/agent-desk/actions/runs/27793545211",
    ),
    inputBinding: RecordedVerificationInputBinding? = recordedVerificationInputBinding(),
): RecordedVerificationResult = RecordedVerificationResult(
    name = WorkSummary.parse(name),
    kind = kind,
    result = result,
    durationMillis = durationMillis,
    outputReference = WorkSummary.parse(outputReference),
    failureSummary = failureSummary?.let(WorkSummary::parse),
    evidenceReference = evidenceReference,
    inputBinding = inputBinding,
)

fun recordedVerificationInputBinding(
    digest: RecordedContentDigest = RecordedContentDigest.parseSha256(
        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
    ),
    algorithm: RecordedDigestAlgorithm = RecordedDigestAlgorithm.Sha256,
    capturedAt: EventTimestamp = WorkEventFixtures.terminalAt,
): RecordedVerificationInputBinding = RecordedVerificationInputBinding(
    digest = digest,
    algorithm = algorithm,
    capturedAt = capturedAt,
)

private fun defaultVerificationResult(capturedAt: EventTimestamp): RecordedVerificationResult = recordedVerificationResult(inputBinding = recordedVerificationInputBinding(capturedAt = capturedAt))

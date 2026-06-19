package com.yonatankarp.agentdesk.app.operator.verification

import com.yonatankarp.agentdesk.core.domain.events.EventTimestamp
import com.yonatankarp.agentdesk.core.domain.events.RecordedDigestAlgorithm
import com.yonatankarp.agentdesk.core.domain.events.RecordedVerificationInputBinding
import com.yonatankarp.agentdesk.core.domain.events.RecordedVerificationKind
import com.yonatankarp.agentdesk.core.domain.events.RecordedVerificationResult
import com.yonatankarp.agentdesk.core.domain.events.RecordedVerificationState
import com.yonatankarp.agentdesk.core.domain.events.WorkEvent
import com.yonatankarp.agentdesk.core.domain.events.WorkVerificationOutcome
import com.yonatankarp.agentdesk.core.domain.events.WorkVerificationRecordedPayload
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemId

data class RecordedCompletionEvidence(
    val checklist: CompletionEvidenceChecklist,
    val lastChangedAt: EventTimestamp?,
)

object RecordedVerificationEvidenceProjector {
    fun project(
        events: List<WorkEvent>,
        workItemId: WorkItemId,
    ): RecordedCompletionEvidence {
        val itemEvents = events.filter { it.workItemId == workItemId }
        val latestVerification = itemEvents
            .filter { it.payload is WorkVerificationRecordedPayload }
            .maxByOrNull { it.occurredAt }

        val lastChangedAt = itemEvents
            .filterNot { it.payload is WorkVerificationRecordedPayload }
            .maxOfOrNull { it.occurredAt }

        return RecordedCompletionEvidence(
            checklist = latestVerification
                ?.let { (it.payload as WorkVerificationRecordedPayload).toChecklist() }
                ?: unknownChecklist(),
            lastChangedAt = lastChangedAt,
        )
    }

    private fun WorkVerificationRecordedPayload.toChecklist(): CompletionEvidenceChecklist = CompletionEvidenceChecklist(
        outcome = outcome.toCompletionOutcome(),
        verificationAttempted = verificationAttempted,
        knownFailures = knownFailures.map { it.toString() },
        touchedArtifacts = touchedArtifacts.map { it.toString() },
        residualRisks = residualRisks.map { it.toString() },
        verificationResults = results.map { it.toVerificationResult() },
    )

    private fun RecordedVerificationResult.toVerificationResult(): VerificationResult = VerificationResult(
        name = name.toString(),
        kind = kind.toVerificationKind(),
        result = result.toVerificationState(),
        durationMillis = durationMillis,
        outputReference = outputReference.toString(),
        failureSummary = failureSummary?.toString(),
        evidenceReference = evidenceReference,
        inputBinding = inputBinding?.toVerificationInputBinding(),
    )

    private fun RecordedVerificationInputBinding.toVerificationInputBinding(): VerificationInputBinding = VerificationInputBinding(
        digest = ContentDigest.parseSha256(digest.toString()),
        algorithm = algorithm.toDigestAlgorithm(),
        capturedAt = capturedAt,
    )

    private fun WorkVerificationOutcome.toCompletionOutcome(): CompletionOutcome = when (this) {
        WorkVerificationOutcome.Ready -> CompletionOutcome.Ready
        WorkVerificationOutcome.NotReady -> CompletionOutcome.NotReady
        WorkVerificationOutcome.Blocked -> CompletionOutcome.Blocked
        WorkVerificationOutcome.Unknown -> CompletionOutcome.Unknown
    }

    private fun RecordedVerificationKind.toVerificationKind(): VerificationKind = when (this) {
        RecordedVerificationKind.LocalTest -> VerificationKind.LocalTest
        RecordedVerificationKind.CiCheck -> VerificationKind.CiCheck
        RecordedVerificationKind.SmokeRun -> VerificationKind.SmokeRun
        RecordedVerificationKind.ManualQa -> VerificationKind.ManualQa
    }

    private fun RecordedVerificationState.toVerificationState(): VerificationState = when (this) {
        RecordedVerificationState.Passed -> VerificationState.Passed
        RecordedVerificationState.Failed -> VerificationState.Failed
        RecordedVerificationState.Skipped -> VerificationState.Skipped
        RecordedVerificationState.Unknown -> VerificationState.Unknown
    }

    private fun RecordedDigestAlgorithm.toDigestAlgorithm(): DigestAlgorithm = when (this) {
        RecordedDigestAlgorithm.Sha256 -> DigestAlgorithm.Sha256
    }

    private fun unknownChecklist(): CompletionEvidenceChecklist = CompletionEvidenceChecklist(
        outcome = CompletionOutcome.Unknown,
        verificationAttempted = false,
        knownFailures = emptyList(),
        touchedArtifacts = emptyList(),
        residualRisks = emptyList(),
        verificationResults = emptyList(),
    )
}

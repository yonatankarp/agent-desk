package com.yonatankarp.agentdesk.app.serialization

import kotlinx.serialization.Serializable

@Serializable
data class WorkEventPayloadRecord(
    val title: String? = null,
    val summary: String? = null,
    val reason: String? = null,
    val outcome: String? = null,
    val verificationAttempted: Boolean? = null,
    val knownFailures: List<String> = emptyList(),
    val touchedArtifacts: List<String> = emptyList(),
    val residualRisks: List<String> = emptyList(),
    val verificationResults: List<VerificationResultRecord> = emptyList(),
)

@Serializable
data class VerificationResultRecord(
    val name: String,
    val kind: String,
    val result: String,
    val durationMillis: Long? = null,
    val outputReference: String,
    val failureSummary: String? = null,
    val evidenceReference: EvidenceReferenceRecord,
    val inputBinding: VerificationInputBindingRecord? = null,
)

@Serializable
data class VerificationInputBindingRecord(
    val digest: String,
    val algorithm: String,
    val capturedAt: String,
)

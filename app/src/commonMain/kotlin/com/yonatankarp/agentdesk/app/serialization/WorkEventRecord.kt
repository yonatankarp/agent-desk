package com.yonatankarp.agentdesk.app.serialization

import kotlinx.serialization.Serializable

@Serializable
data class WorkEventRecord(
    val id: String,
    val occurredAt: String,
    val source: String,
    val workItemId: String,
    val type: String,
    val payload: WorkEventPayloadRecord = WorkEventPayloadRecord(),
    val evidenceReferences: List<EvidenceReferenceRecord> = emptyList(),
    val provenance: WorkProvenanceRecord? = null,
)

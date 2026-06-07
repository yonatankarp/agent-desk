package com.yonatankarp.agentdesk.app.serialization

import kotlinx.serialization.Serializable

@Serializable
data class AuditRecordRecord(
    val id: String,
    val actor: String,
    val actorKind: String,
    val timestamp: String,
    val recordedAt: String,
    val action: String,
    val target: String,
    val result: String,
    val sourceItem: String,
    val correlationId: String,
    val evidenceReference: EvidenceReferenceRecord,
    val detail: String,
)

package com.yonatankarp.agentdesk.app.operator

data class EventLine(
    val occurredAt: String,
    val type: String,
    val workItemId: String,
    val source: String,
    val detail: String,
    val evidenceReferences: List<EvidenceLine> = emptyList(),
    val provenance: ProvenanceLine? = null,
)

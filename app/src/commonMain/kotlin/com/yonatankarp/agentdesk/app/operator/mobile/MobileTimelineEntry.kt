package com.yonatankarp.agentdesk.app.operator.mobile

data class MobileTimelineEntry(
    val eventId: String,
    val occurredAt: String,
    val timeWindow: String,
    val source: String,
    val workItemId: String,
    val type: String,
    val statusLabel: String,
    val stateLabel: String,
    val summary: String,
    val completionSummary: String?,
    val evidenceReferences: List<MobileEvidenceReference> = emptyList(),
    val provenance: MobileProvenance? = null,
)

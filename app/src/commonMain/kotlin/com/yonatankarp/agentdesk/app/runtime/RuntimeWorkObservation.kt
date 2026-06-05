package com.yonatankarp.agentdesk.app.runtime

data class RuntimeWorkObservation(
    val eventId: String,
    val occurredAt: String,
    val source: String,
    val workItemId: String,
    val kind: RuntimeWorkObservationKind,
    val title: String? = null,
    val summary: String? = null,
    val reason: String? = null,
    val evidenceReferences: List<RuntimeEvidenceReference> = emptyList(),
)

data class RuntimeEvidenceReference(
    val kind: String,
    val label: String,
    val target: String,
)

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
    val provenance: RuntimeWorkProvenance? = null,
)

data class RuntimeEvidenceReference(
    val kind: String,
    val label: String,
    val target: String,
)

data class RuntimeWorkProvenance(
    val projectId: String? = null,
    val workspaceId: String? = null,
    val sourceId: String? = null,
    val ownerId: String? = null,
    val agentId: String? = null,
    val modelId: String? = null,
    val toolId: String? = null,
    val runId: String? = null,
    val objectiveId: String? = null,
    val parentHandoffId: String? = null,
    val archiveRecordId: String? = null,
)

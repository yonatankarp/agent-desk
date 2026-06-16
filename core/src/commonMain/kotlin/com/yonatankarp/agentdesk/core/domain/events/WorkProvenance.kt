package com.yonatankarp.agentdesk.core.domain.events

import com.yonatankarp.agentdesk.core.domain.valueobjects.IdentifierGrammar

data class WorkProvenance(
    val projectId: ProvenanceId? = null,
    val workspaceId: ProvenanceId? = null,
    val sourceId: ProvenanceId? = null,
    val ownerId: ProvenanceId? = null,
    val agentId: ProvenanceId? = null,
    val modelId: ProvenanceId? = null,
    val toolId: ProvenanceId? = null,
    val runId: ProvenanceId? = null,
    val objectiveId: ProvenanceId? = null,
    val parentHandoffId: ProvenanceId? = null,
    val archiveRecordId: ProvenanceId? = null,
)

@JvmInline
value class ProvenanceId private constructor(val value: String) {
    companion object {
        fun parse(raw: String): ProvenanceId = ProvenanceId(
            IdentifierGrammar.normalize(
                raw = raw,
                fieldName = "Provenance id",
                errorMessage = "Provenance id must be a lowercase public-safe identifier",
            ),
        )
    }

    override fun toString(): String = value
}

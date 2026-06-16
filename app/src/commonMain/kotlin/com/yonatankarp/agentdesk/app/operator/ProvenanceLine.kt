package com.yonatankarp.agentdesk.app.operator

import com.yonatankarp.agentdesk.core.domain.events.WorkProvenance

data class ProvenanceLine(
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
) {
    companion object {
        fun from(provenance: WorkProvenance?): ProvenanceLine? = provenance?.let {
            ProvenanceLine(
                projectId = it.projectId?.toString(),
                workspaceId = it.workspaceId?.toString(),
                sourceId = it.sourceId?.toString(),
                ownerId = it.ownerId?.toString(),
                agentId = it.agentId?.toString(),
                modelId = it.modelId?.toString(),
                toolId = it.toolId?.toString(),
                runId = it.runId?.toString(),
                objectiveId = it.objectiveId?.toString(),
                parentHandoffId = it.parentHandoffId?.toString(),
                archiveRecordId = it.archiveRecordId?.toString(),
            )
        }
    }
}

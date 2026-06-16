package com.yonatankarp.agentdesk.app.operator.mobile

import com.yonatankarp.agentdesk.app.operator.ProvenanceLine

data class MobileProvenance(
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
        fun from(line: ProvenanceLine?): MobileProvenance? = line?.let {
            MobileProvenance(
                projectId = it.projectId,
                workspaceId = it.workspaceId,
                sourceId = it.sourceId,
                ownerId = it.ownerId,
                agentId = it.agentId,
                modelId = it.modelId,
                toolId = it.toolId,
                runId = it.runId,
                objectiveId = it.objectiveId,
                parentHandoffId = it.parentHandoffId,
                archiveRecordId = it.archiveRecordId,
            )
        }
    }
}

package com.yonatankarp.agentdesk.app.serialization

import kotlinx.serialization.Serializable

@Serializable
data class WorkProvenanceRecord(
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

package com.yonatankarp.agentdesk.core.domain.serialization

import kotlinx.serialization.Serializable

@Serializable
data class WorkEventRecord(
    val id: String,
    val occurredAt: String,
    val source: String,
    val workItemId: String,
    val type: String,
    val payload: WorkEventPayloadRecord = WorkEventPayloadRecord(),
)

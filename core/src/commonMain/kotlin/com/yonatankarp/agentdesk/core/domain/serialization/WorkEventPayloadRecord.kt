package com.yonatankarp.agentdesk.core.domain.serialization

import kotlinx.serialization.Serializable

@Serializable
data class WorkEventPayloadRecord(
    val title: String? = null,
    val summary: String? = null,
    val reason: String? = null,
)

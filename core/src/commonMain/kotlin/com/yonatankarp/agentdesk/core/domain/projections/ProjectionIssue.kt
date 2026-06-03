package com.yonatankarp.agentdesk.core.domain.projections

import com.yonatankarp.agentdesk.core.domain.events.WorkEventId

data class ProjectionIssue(
    val eventId: WorkEventId,
    val reason: String,
)

package com.yonatankarp.agentdesk.core.domain.projections

import com.yonatankarp.agentdesk.core.domain.entities.WorkItem
import com.yonatankarp.agentdesk.core.domain.events.WorkEvent

data class OperatorStateProjection(
    val workItems: List<WorkItem>,
    val recentEvents: List<WorkEvent>,
    val ignoredEvents: List<ProjectionIssue>,
    val staleAttention: List<StaleWorkAttention> = emptyList(),
)

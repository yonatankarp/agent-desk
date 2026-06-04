package com.yonatankarp.agentdesk.app.operator

import com.yonatankarp.agentdesk.core.domain.entities.WorkItem
import com.yonatankarp.agentdesk.core.domain.events.WorkEvent
import com.yonatankarp.agentdesk.core.domain.projections.StaleWorkAttention

data class OperatorState(
    val workItems: List<WorkItem>,
    val events: List<WorkEvent>,
    val staleAttention: List<StaleWorkAttention> = emptyList(),
)

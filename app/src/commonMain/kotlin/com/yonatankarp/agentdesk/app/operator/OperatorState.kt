package com.yonatankarp.agentdesk.app.operator

import com.yonatankarp.agentdesk.core.domain.entities.WorkItem
import com.yonatankarp.agentdesk.core.domain.events.WorkEvent
import com.yonatankarp.agentdesk.core.domain.projections.OperatorStateProjection
import com.yonatankarp.agentdesk.core.domain.projections.StaleWorkAttention

data class OperatorState(
    val workItems: List<WorkItem>,
    val events: List<WorkEvent>,
    val staleAttention: List<StaleWorkAttention> = emptyList(),
    val storeReadWarning: String? = null,
) {
    companion object {
        fun from(projection: OperatorStateProjection): OperatorState = OperatorState(
            workItems = projection.workItems,
            events = projection.recentEvents,
            staleAttention = projection.staleAttention,
        )
    }
}

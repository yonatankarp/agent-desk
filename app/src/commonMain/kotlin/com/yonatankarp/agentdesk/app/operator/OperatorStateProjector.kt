package com.yonatankarp.agentdesk.app.operator

import com.yonatankarp.agentdesk.core.domain.events.WorkEvent
import com.yonatankarp.agentdesk.core.domain.projections.WorkEventProjector

object OperatorStateProjector {
    fun project(events: List<WorkEvent>): OperatorState {
        val projection = WorkEventProjector.project(events)
        val issue = projection.ignoredEvents.firstOrNull()
        if (issue != null) {
            throw OperatorStateProjectionException("Invalid event sequence: ${issue.reason}.")
        }

        return OperatorState(
            workItems = projection.workItems,
            events = projection.recentEvents,
        )
    }
}

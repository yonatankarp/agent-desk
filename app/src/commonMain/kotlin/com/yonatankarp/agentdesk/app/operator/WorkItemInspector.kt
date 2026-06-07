package com.yonatankarp.agentdesk.app.operator

import com.yonatankarp.agentdesk.core.domain.events.WorkEvent
import com.yonatankarp.agentdesk.core.domain.projections.WorkEventProjector
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemId

object WorkItemInspector {
    fun inspect(
        events: List<WorkEvent>,
        workItemId: WorkItemId,
    ): WorkItemInspection? {
        val projection = WorkEventProjector.project(events)
        val state = OperatorState.from(projection)
        val eventsById = events.associateBy { it.id }
        val warnings = projection.ignoredEvents.mapNotNull { issue ->
            val event = eventsById[issue.eventId]
            if (event?.workItemId != workItemId) {
                null
            } else {
                ProjectionWarningLine(
                    eventId = issue.eventId.toString(),
                    reason = issue.reason,
                )
            }
        }

        return inspect(state, workItemId, warnings)
    }

    fun inspect(
        state: OperatorState,
        workItemId: WorkItemId,
        projectionWarnings: List<ProjectionWarningLine> = emptyList(),
    ): WorkItemInspection? {
        val item = state.workItems.firstOrNull { it.id == workItemId } ?: return null
        return WorkItemInspection(
            item = item,
            statusPresentation = OperatorStatePresenter.presentationFor(item.status),
            requiresAttention = item.status.requiresHumanAttention,
            isTerminal = item.status.isTerminal,
            acceptedEvents = OperatorStatePresenter.eventLines(state).filter { it.workItemId == workItemId.toString() },
            projectionWarnings = projectionWarnings,
        )
    }
}

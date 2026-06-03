package com.yonatankarp.agentdesk.core.domain.projections

import com.yonatankarp.agentdesk.core.domain.entities.WorkItem
import com.yonatankarp.agentdesk.core.domain.events.WorkBlockedPayload
import com.yonatankarp.agentdesk.core.domain.events.WorkCanceledPayload
import com.yonatankarp.agentdesk.core.domain.events.WorkEvent
import com.yonatankarp.agentdesk.core.domain.events.WorkEventId
import com.yonatankarp.agentdesk.core.domain.events.WorkFailedPayload
import com.yonatankarp.agentdesk.core.domain.events.WorkNeedsDecisionPayload
import com.yonatankarp.agentdesk.core.domain.events.WorkStartedPayload
import com.yonatankarp.agentdesk.core.domain.events.WorkSucceededPayload
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemId
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkStatus

object WorkEventProjector {
    fun project(events: List<WorkEvent>): OperatorStateProjection {
        val seenEventIds = mutableSetOf<WorkEventId>()
        val workItems = linkedMapOf<WorkItemId, WorkItem>()
        val acceptedEvents = mutableListOf<WorkEvent>()
        val ignoredEvents = mutableListOf<ProjectionIssue>()

        events.forEach { event ->
            if (!seenEventIds.add(event.id)) {
                ignoredEvents += ProjectionIssue(event.id, "Duplicate event id ignored")
                return@forEach
            }

            val current = workItems[event.workItemId]
            val next = event.nextWorkItem(current)
            if (next == null) {
                ignoredEvents += ProjectionIssue(event.id, "Event requires an existing started work item")
                return@forEach
            }

            val transition = current.transitionTo(next, event.id)
            if (transition is ProjectionTransition.Ignored) {
                ignoredEvents += transition.issue
                return@forEach
            }

            workItems[event.workItemId] = next
            acceptedEvents += event
        }

        return OperatorStateProjection(
            workItems = workItems.values.toList(),
            recentEvents = acceptedEvents,
            ignoredEvents = ignoredEvents,
        )
    }

    private fun WorkEvent.nextWorkItem(current: WorkItem?): WorkItem? = when (val payload = payload) {
        is WorkStartedPayload ->
            WorkItem(
                id = workItemId,
                title = payload.title,
                status = WorkStatus.Running,
                summary = payload.summary,
            )

        is WorkNeedsDecisionPayload ->
            current?.copy(
                status = WorkStatus.NeedsDecision,
                summary = payload.reason,
            )

        is WorkBlockedPayload ->
            current?.copy(
                status = WorkStatus.Blocked,
                summary = payload.reason,
            )

        WorkSucceededPayload ->
            current?.copy(
                status = WorkStatus.Succeeded,
            )

        is WorkFailedPayload ->
            current?.copy(
                status = WorkStatus.Failed,
                summary = payload.reason,
            )

        is WorkCanceledPayload ->
            current?.copy(
                status = WorkStatus.Canceled,
                summary = payload.reason ?: current.summary,
            )
    }

    private fun WorkItem?.transitionTo(
        next: WorkItem,
        eventId: WorkEventId,
    ): ProjectionTransition {
        if (this == null) {
            return ProjectionTransition.Accepted
        }

        return if (status.canTransitionTo(next.status)) {
            ProjectionTransition.Accepted
        } else {
            ProjectionTransition.Ignored(
                ProjectionIssue(eventId, "Cannot transition work item $id from $status to ${next.status}"),
            )
        }
    }

    private sealed interface ProjectionTransition {
        data object Accepted : ProjectionTransition

        data class Ignored(val issue: ProjectionIssue) : ProjectionTransition
    }
}

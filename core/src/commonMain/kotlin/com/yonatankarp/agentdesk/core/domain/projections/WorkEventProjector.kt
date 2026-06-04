package com.yonatankarp.agentdesk.core.domain.projections

import com.yonatankarp.agentdesk.core.domain.entities.WorkItem
import com.yonatankarp.agentdesk.core.domain.events.EventTimestamp
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
    fun project(
        events: List<WorkEvent>,
        staleThreshold: StaleWorkThreshold = StaleWorkThreshold.default,
    ): OperatorStateProjection {
        val seenEventIds = mutableSetOf<WorkEventId>()
        val workItems = linkedMapOf<WorkItemId, WorkItem>()
        val latestEventByWorkItem = mutableMapOf<WorkItemId, WorkEvent>()
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
            latestEventByWorkItem[event.workItemId] = event
            acceptedEvents += event
        }

        return OperatorStateProjection(
            workItems = workItems.values.toList(),
            recentEvents = acceptedEvents,
            ignoredEvents = ignoredEvents,
            staleAttention = deriveStaleAttention(
                workItems = workItems,
                latestEventByWorkItem = latestEventByWorkItem,
                acceptedEvents = acceptedEvents,
                staleThreshold = staleThreshold,
            ),
        )
    }

    private fun deriveStaleAttention(
        workItems: Map<WorkItemId, WorkItem>,
        latestEventByWorkItem: Map<WorkItemId, WorkEvent>,
        acceptedEvents: List<WorkEvent>,
        staleThreshold: StaleWorkThreshold,
    ): List<StaleWorkAttention> {
        val referenceMinute = acceptedEvents.maxOfOrNull { it.occurredAt.epochMinute() } ?: return emptyList()

        return workItems.values.mapNotNull { item ->
            if (item.status !in staleEligibleStatuses) {
                return@mapNotNull null
            }

            val latestEvent = latestEventByWorkItem[item.id] ?: return@mapNotNull null
            val staleForMinutes = referenceMinute - latestEvent.occurredAt.epochMinute()
            if (staleForMinutes < staleThreshold.minutes) {
                return@mapNotNull null
            }

            StaleWorkAttention(
                workItemId = item.id,
                status = item.status,
                lastEventAt = latestEvent.occurredAt,
                staleForMinutes = staleForMinutes,
            )
        }
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

    private fun EventTimestamp.epochMinute(): Long {
        val text = value
        val year = text.substring(0, 4).toInt()
        val month = text.substring(5, 7).toInt()
        val day = text.substring(8, 10).toInt()
        val hour = text.substring(11, 13).toInt()
        val minute = text.substring(14, 16).toInt()
        return daysFromCivil(year, month, day) * 24L * 60L + hour * 60L + minute
    }

    private fun daysFromCivil(
        year: Int,
        month: Int,
        day: Int,
    ): Long {
        val adjustedYear = if (month <= 2) year - 1 else year
        val era = floorDiv(adjustedYear, 400)
        val yearOfEra = adjustedYear - era * 400
        val monthPrime = month + if (month > 2) -3 else 9
        val dayOfYear = (153 * monthPrime + 2) / 5 + day - 1
        val dayOfEra = yearOfEra * 365 + yearOfEra / 4 - yearOfEra / 100 + dayOfYear
        return (era * 146097 + dayOfEra - 719468).toLong()
    }

    private fun floorDiv(
        value: Int,
        divisor: Int,
    ): Int {
        val quotient = value / divisor
        val remainder = value % divisor
        return if (remainder != 0 && (value xor divisor) < 0) quotient - 1 else quotient
    }

    private val staleEligibleStatuses = setOf(WorkStatus.Running, WorkStatus.Waiting)
}

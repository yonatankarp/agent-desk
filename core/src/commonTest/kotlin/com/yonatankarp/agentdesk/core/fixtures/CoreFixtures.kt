package com.yonatankarp.agentdesk.core.fixtures

import com.yonatankarp.agentdesk.core.domain.entities.WorkItem
import com.yonatankarp.agentdesk.core.domain.events.EventSource
import com.yonatankarp.agentdesk.core.domain.events.EventTimestamp
import com.yonatankarp.agentdesk.core.domain.events.WorkBlockedPayload
import com.yonatankarp.agentdesk.core.domain.events.WorkEvent
import com.yonatankarp.agentdesk.core.domain.events.WorkEventId
import com.yonatankarp.agentdesk.core.domain.events.WorkEventPayload
import com.yonatankarp.agentdesk.core.domain.events.WorkStartedPayload
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemId
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemTitle
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkStatus
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkSummary

internal object CoreFixtures {
    val workItemId: WorkItemId = WorkItemId.parse("agent-task:42")
    val workTitle: WorkItemTitle = WorkItemTitle.parse("Run public hygiene check")
    val startedSummary: WorkSummary = WorkSummary.parse("Agent accepted the task and started local checks.")
    val blockedReason: WorkSummary = WorkSummary.parse("CI failed on the core test task.")
    val eventSource: EventSource = EventSource.parse("mock-adapter")
    val startedAt: EventTimestamp = EventTimestamp.parse("2026-06-02T21:00:00Z")
    val blockedAt: EventTimestamp = EventTimestamp.parse("2026-06-02T21:05:00.123Z")

    fun workItem(
        status: WorkStatus = WorkStatus.Queued,
        id: WorkItemId = workItemId,
        title: WorkItemTitle = workTitle,
        summary: WorkSummary? = null,
    ): WorkItem = WorkItem(
        id = id,
        title = title,
        status = status,
        summary = summary,
    )

    fun workStartedEvent(
        id: WorkEventId = WorkEventId.parse("event:agent-task:42:started"),
        occurredAt: EventTimestamp = startedAt,
        source: EventSource = eventSource,
        workItemId: WorkItemId = CoreFixtures.workItemId,
        payload: WorkEventPayload = WorkStartedPayload(
            title = workTitle,
            summary = startedSummary,
        ),
    ): WorkEvent = WorkEvent(
        id = id,
        occurredAt = occurredAt,
        source = source,
        workItemId = workItemId,
        payload = payload,
    )

    fun workBlockedEvent(
        id: WorkEventId = WorkEventId.parse("event:agent-task:42:blocked"),
        occurredAt: EventTimestamp = blockedAt,
        source: EventSource = eventSource,
        workItemId: WorkItemId = CoreFixtures.workItemId,
        payload: WorkEventPayload = WorkBlockedPayload(reason = blockedReason),
    ): WorkEvent = WorkEvent(
        id = id,
        occurredAt = occurredAt,
        source = source,
        workItemId = workItemId,
        payload = payload,
    )
}

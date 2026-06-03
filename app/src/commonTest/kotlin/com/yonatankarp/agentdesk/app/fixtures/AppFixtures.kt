package com.yonatankarp.agentdesk.app.fixtures

import com.yonatankarp.agentdesk.core.domain.events.EventSource
import com.yonatankarp.agentdesk.core.domain.events.EventTimestamp
import com.yonatankarp.agentdesk.core.domain.events.WorkBlockedPayload
import com.yonatankarp.agentdesk.core.domain.events.WorkCanceledPayload
import com.yonatankarp.agentdesk.core.domain.events.WorkEvent
import com.yonatankarp.agentdesk.core.domain.events.WorkEventId
import com.yonatankarp.agentdesk.core.domain.events.WorkEventPayload
import com.yonatankarp.agentdesk.core.domain.events.WorkFailedPayload
import com.yonatankarp.agentdesk.core.domain.events.WorkNeedsDecisionPayload
import com.yonatankarp.agentdesk.core.domain.events.WorkStartedPayload
import com.yonatankarp.agentdesk.core.domain.events.WorkSucceededPayload
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemId
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemTitle
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkSummary

internal object AppFixtures {
    val workItemId: WorkItemId = WorkItemId.parse("agent-task:42")
    val workTitle: WorkItemTitle = WorkItemTitle.parse("Run public hygiene check")
    val startedSummary: WorkSummary = WorkSummary.parse("Agent accepted the task and started local checks.")
    val blockedReason: WorkSummary = WorkSummary.parse("CI failed on the core test task.")
    val eventSource: EventSource = EventSource.parse("mock-adapter")
    val startedAt: EventTimestamp = EventTimestamp.parse("2026-06-02T21:00:00Z")
    val needsDecisionAt: EventTimestamp = EventTimestamp.parse("2026-06-02T21:03:00Z")
    val blockedAt: EventTimestamp = EventTimestamp.parse("2026-06-02T21:05:00.123Z")
    val terminalAt: EventTimestamp = EventTimestamp.parse("2026-06-02T21:10:00Z")

    fun workStartedEvent(
        id: WorkEventId = WorkEventId.parse("event:agent-task:42:started"),
        occurredAt: EventTimestamp = startedAt,
        source: EventSource = eventSource,
        workItemId: WorkItemId = AppFixtures.workItemId,
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
        workItemId: WorkItemId = AppFixtures.workItemId,
        payload: WorkEventPayload = WorkBlockedPayload(reason = blockedReason),
    ): WorkEvent = WorkEvent(
        id = id,
        occurredAt = occurredAt,
        source = source,
        workItemId = workItemId,
        payload = payload,
    )

    fun workNeedsDecisionEvent(
        id: WorkEventId = WorkEventId.parse("event:agent-task:42:needs-decision"),
        occurredAt: EventTimestamp = needsDecisionAt,
        source: EventSource = eventSource,
        workItemId: WorkItemId = AppFixtures.workItemId,
        payload: WorkEventPayload = WorkNeedsDecisionPayload(reason = WorkSummary.parse("Operator decision needed.")),
    ): WorkEvent = WorkEvent(
        id = id,
        occurredAt = occurredAt,
        source = source,
        workItemId = workItemId,
        payload = payload,
    )

    fun workSucceededEvent(
        id: WorkEventId = WorkEventId.parse("event:agent-task:42:succeeded"),
        occurredAt: EventTimestamp = terminalAt,
        source: EventSource = eventSource,
        workItemId: WorkItemId = AppFixtures.workItemId,
        payload: WorkEventPayload = WorkSucceededPayload,
    ): WorkEvent = WorkEvent(
        id = id,
        occurredAt = occurredAt,
        source = source,
        workItemId = workItemId,
        payload = payload,
    )

    fun workFailedEvent(
        id: WorkEventId = WorkEventId.parse("event:agent-task:42:failed"),
        occurredAt: EventTimestamp = terminalAt,
        source: EventSource = eventSource,
        workItemId: WorkItemId = AppFixtures.workItemId,
        payload: WorkEventPayload = WorkFailedPayload(reason = WorkSummary.parse("Build failed.")),
    ): WorkEvent = WorkEvent(
        id = id,
        occurredAt = occurredAt,
        source = source,
        workItemId = workItemId,
        payload = payload,
    )

    fun workCanceledEvent(
        id: WorkEventId = WorkEventId.parse("event:agent-task:42:canceled"),
        occurredAt: EventTimestamp = terminalAt,
        source: EventSource = eventSource,
        workItemId: WorkItemId = AppFixtures.workItemId,
        payload: WorkEventPayload = WorkCanceledPayload(reason = WorkSummary.parse("Operator canceled the task.")),
    ): WorkEvent = WorkEvent(
        id = id,
        occurredAt = occurredAt,
        source = source,
        workItemId = workItemId,
        payload = payload,
    )
}

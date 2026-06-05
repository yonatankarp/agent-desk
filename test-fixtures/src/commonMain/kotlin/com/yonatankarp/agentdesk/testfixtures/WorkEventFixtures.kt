package com.yonatankarp.agentdesk.testfixtures

import com.yonatankarp.agentdesk.core.domain.entities.WorkItem
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
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkStatus
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkSummary

interface CanonicalWorkEventFixtures {
    val workItemId: WorkItemId
    val workTitle: WorkItemTitle
    val startedSummary: WorkSummary
    val blockedReason: WorkSummary
    val eventSource: EventSource
    val startedAt: EventTimestamp
    val needsDecisionAt: EventTimestamp
    val blockedAt: EventTimestamp
    val terminalAt: EventTimestamp

    fun workItem(
        status: WorkStatus = WorkStatus.Queued,
        id: WorkItemId = this.workItemId,
        title: WorkItemTitle = this.workTitle,
        summary: WorkSummary? = null,
    ): WorkItem

    fun workStartedEvent(
        id: WorkEventId = WorkEventId.parse("event:agent-task:42:started"),
        occurredAt: EventTimestamp = this.startedAt,
        source: EventSource = this.eventSource,
        workItemId: WorkItemId = this.workItemId,
        payload: WorkEventPayload = WorkStartedPayload(
            title = this.workTitle,
            summary = this.startedSummary,
        ),
    ): WorkEvent

    fun workBlockedEvent(
        id: WorkEventId = WorkEventId.parse("event:agent-task:42:blocked"),
        occurredAt: EventTimestamp = this.blockedAt,
        source: EventSource = this.eventSource,
        workItemId: WorkItemId = this.workItemId,
        payload: WorkEventPayload = WorkBlockedPayload(reason = this.blockedReason),
    ): WorkEvent

    fun workNeedsDecisionEvent(
        id: WorkEventId = WorkEventId.parse("event:agent-task:42:needs-decision"),
        occurredAt: EventTimestamp = this.needsDecisionAt,
        source: EventSource = this.eventSource,
        workItemId: WorkItemId = this.workItemId,
        payload: WorkEventPayload = WorkNeedsDecisionPayload(reason = WorkSummary.parse("Operator decision needed.")),
    ): WorkEvent

    fun workSucceededEvent(
        id: WorkEventId = WorkEventId.parse("event:agent-task:42:succeeded"),
        occurredAt: EventTimestamp = this.terminalAt,
        source: EventSource = this.eventSource,
        workItemId: WorkItemId = this.workItemId,
        payload: WorkEventPayload = WorkSucceededPayload,
    ): WorkEvent

    fun workFailedEvent(
        id: WorkEventId = WorkEventId.parse("event:agent-task:42:failed"),
        occurredAt: EventTimestamp = this.terminalAt,
        source: EventSource = this.eventSource,
        workItemId: WorkItemId = this.workItemId,
        payload: WorkEventPayload = WorkFailedPayload(reason = WorkSummary.parse("Build failed.")),
    ): WorkEvent

    fun workCanceledEvent(
        id: WorkEventId = WorkEventId.parse("event:agent-task:42:canceled"),
        occurredAt: EventTimestamp = this.terminalAt,
        source: EventSource = this.eventSource,
        workItemId: WorkItemId = this.workItemId,
        payload: WorkEventPayload = WorkCanceledPayload(reason = WorkSummary.parse("Operator canceled the task.")),
    ): WorkEvent
}

object WorkEventFixtures : CanonicalWorkEventFixtures {
    override val workItemId: WorkItemId = WorkItemId.parse("agent-task:42")
    override val workTitle: WorkItemTitle = WorkItemTitle.parse("Run public hygiene check")
    override val startedSummary: WorkSummary = WorkSummary.parse("Agent accepted the task and started local checks.")
    override val blockedReason: WorkSummary = WorkSummary.parse("CI failed on the core test task.")
    override val eventSource: EventSource = EventSource.parse("mock-adapter")
    override val startedAt: EventTimestamp = EventTimestamp.parse("2026-06-02T21:00:00Z")
    override val needsDecisionAt: EventTimestamp = EventTimestamp.parse("2026-06-02T21:03:00Z")
    override val blockedAt: EventTimestamp = EventTimestamp.parse("2026-06-02T21:05:00.123Z")
    override val terminalAt: EventTimestamp = EventTimestamp.parse("2026-06-02T21:10:00Z")

    override fun workItem(
        status: WorkStatus,
        id: WorkItemId,
        title: WorkItemTitle,
        summary: WorkSummary?,
    ): WorkItem = WorkItem(
        id = id,
        title = title,
        status = status,
        summary = summary,
    )

    override fun workStartedEvent(
        id: WorkEventId,
        occurredAt: EventTimestamp,
        source: EventSource,
        workItemId: WorkItemId,
        payload: WorkEventPayload,
    ): WorkEvent = WorkEvent(
        id = id,
        occurredAt = occurredAt,
        source = source,
        workItemId = workItemId,
        payload = payload,
    )

    override fun workBlockedEvent(
        id: WorkEventId,
        occurredAt: EventTimestamp,
        source: EventSource,
        workItemId: WorkItemId,
        payload: WorkEventPayload,
    ): WorkEvent = WorkEvent(
        id = id,
        occurredAt = occurredAt,
        source = source,
        workItemId = workItemId,
        payload = payload,
    )

    override fun workNeedsDecisionEvent(
        id: WorkEventId,
        occurredAt: EventTimestamp,
        source: EventSource,
        workItemId: WorkItemId,
        payload: WorkEventPayload,
    ): WorkEvent = WorkEvent(
        id = id,
        occurredAt = occurredAt,
        source = source,
        workItemId = workItemId,
        payload = payload,
    )

    override fun workSucceededEvent(
        id: WorkEventId,
        occurredAt: EventTimestamp,
        source: EventSource,
        workItemId: WorkItemId,
        payload: WorkEventPayload,
    ): WorkEvent = WorkEvent(
        id = id,
        occurredAt = occurredAt,
        source = source,
        workItemId = workItemId,
        payload = payload,
    )

    override fun workFailedEvent(
        id: WorkEventId,
        occurredAt: EventTimestamp,
        source: EventSource,
        workItemId: WorkItemId,
        payload: WorkEventPayload,
    ): WorkEvent = WorkEvent(
        id = id,
        occurredAt = occurredAt,
        source = source,
        workItemId = workItemId,
        payload = payload,
    )

    override fun workCanceledEvent(
        id: WorkEventId,
        occurredAt: EventTimestamp,
        source: EventSource,
        workItemId: WorkItemId,
        payload: WorkEventPayload,
    ): WorkEvent = WorkEvent(
        id = id,
        occurredAt = occurredAt,
        source = source,
        workItemId = workItemId,
        payload = payload,
    )
}

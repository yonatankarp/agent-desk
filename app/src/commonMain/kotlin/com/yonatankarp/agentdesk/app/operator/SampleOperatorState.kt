package com.yonatankarp.agentdesk.app.operator

import com.yonatankarp.agentdesk.core.domain.events.EventSource
import com.yonatankarp.agentdesk.core.domain.events.EventTimestamp
import com.yonatankarp.agentdesk.core.domain.events.WorkBlockedPayload
import com.yonatankarp.agentdesk.core.domain.events.WorkEvent
import com.yonatankarp.agentdesk.core.domain.events.WorkEventId
import com.yonatankarp.agentdesk.core.domain.events.WorkNeedsDecisionPayload
import com.yonatankarp.agentdesk.core.domain.events.WorkStartedPayload
import com.yonatankarp.agentdesk.core.domain.projections.WorkEventProjector
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemId
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemTitle
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkSummary

object SampleOperatorState {
    fun current(): OperatorState {
        val runningId = WorkItemId.parse("agent-task:42")
        val decisionId = WorkItemId.parse("agent-task:43")
        val blockedId = WorkItemId.parse("agent-task:44")

        val events =
            listOf(
                WorkEvent(
                    id = WorkEventId.parse("event:agent-task:42:started"),
                    occurredAt = EventTimestamp.parse("2026-06-02T21:00:00Z"),
                    source = EventSource.parse("sample-agent"),
                    workItemId = runningId,
                    payload = WorkStartedPayload(
                        title = WorkItemTitle.parse("Run public hygiene check"),
                        summary = WorkSummary.parse("Agent accepted the task and started local checks."),
                    ),
                ),
                WorkEvent(
                    id = WorkEventId.parse("event:agent-task:43:started"),
                    occurredAt = EventTimestamp.parse("2026-06-02T21:02:00Z"),
                    source = EventSource.parse("sample-agent"),
                    workItemId = decisionId,
                    payload = WorkStartedPayload(
                        title = WorkItemTitle.parse("Choose adapter boundary"),
                        summary = WorkSummary.parse("Agent started adapter boundary analysis."),
                    ),
                ),
                WorkEvent(
                    id = WorkEventId.parse("event:agent-task:43:needs-decision"),
                    occurredAt = EventTimestamp.parse("2026-06-02T21:04:00Z"),
                    source = EventSource.parse("sample-agent"),
                    workItemId = decisionId,
                    payload = WorkNeedsDecisionPayload(
                        reason = WorkSummary.parse("Operator decision needed before adapter implementation."),
                    ),
                ),
                WorkEvent(
                    id = WorkEventId.parse("event:agent-task:44:started"),
                    occurredAt = EventTimestamp.parse("2026-06-02T21:04:30Z"),
                    source = EventSource.parse("sample-agent"),
                    workItemId = blockedId,
                    payload = WorkStartedPayload(
                        title = WorkItemTitle.parse("Review build failure"),
                        summary = WorkSummary.parse("Agent started reviewing the build failure."),
                    ),
                ),
                WorkEvent(
                    id = WorkEventId.parse("event:agent-task:44:blocked"),
                    occurredAt = EventTimestamp.parse("2026-06-02T21:05:00Z"),
                    source = EventSource.parse("sample-agent"),
                    workItemId = blockedId,
                    payload = WorkBlockedPayload(
                        reason = WorkSummary.parse("CI failed on the core test task."),
                    ),
                ),

            )

        val projection = WorkEventProjector.project(events)
        return OperatorState(
            workItems = projection.workItems,
            events = projection.recentEvents,
        )
    }
}

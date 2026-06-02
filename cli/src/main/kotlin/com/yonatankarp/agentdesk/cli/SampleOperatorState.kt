package com.yonatankarp.agentdesk.cli

import com.yonatankarp.agentdesk.core.domain.entities.WorkItem
import com.yonatankarp.agentdesk.core.domain.events.EventSource
import com.yonatankarp.agentdesk.core.domain.events.EventTimestamp
import com.yonatankarp.agentdesk.core.domain.events.WorkBlockedPayload
import com.yonatankarp.agentdesk.core.domain.events.WorkEvent
import com.yonatankarp.agentdesk.core.domain.events.WorkEventId
import com.yonatankarp.agentdesk.core.domain.events.WorkStartedPayload
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemId
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemTitle
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkStatus
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkSummary

object SampleOperatorState {
    fun current(): OperatorState {
        val hygieneCheckId = WorkItemId.parse("agent-task:42")
        val adapterReviewId = WorkItemId.parse("agent-task:43")
        val buildReviewId = WorkItemId.parse("agent-task:44")

        return OperatorState(
            workItems = listOf(
                WorkItem(
                    id = hygieneCheckId,
                    title = WorkItemTitle.parse("Run public hygiene check"),
                    status = WorkStatus.Running,
                    summary = WorkSummary.parse("Agent accepted the task and started local checks."),
                ),
                WorkItem(
                    id = adapterReviewId,
                    title = WorkItemTitle.parse("Choose adapter boundary"),
                    status = WorkStatus.NeedsDecision,
                    summary = WorkSummary.parse("Operator decision needed before adapter implementation."),
                ),
                WorkItem(
                    id = buildReviewId,
                    title = WorkItemTitle.parse("Review build failure"),
                    status = WorkStatus.Blocked,
                    summary = WorkSummary.parse("CI failed on the core test task."),
                ),
            ),
            events = listOf(
                WorkEvent(
                    id = WorkEventId.parse("event:agent-task:42:started"),
                    occurredAt = EventTimestamp.parse("2026-06-02T21:00:00Z"),
                    source = EventSource.parse("sample-agent"),
                    workItemId = hygieneCheckId,
                    payload = WorkStartedPayload(
                        title = WorkItemTitle.parse("Run public hygiene check"),
                        summary = WorkSummary.parse("Agent accepted the task and started local checks."),
                    ),
                ),
                WorkEvent(
                    id = WorkEventId.parse("event:agent-task:44:blocked"),
                    occurredAt = EventTimestamp.parse("2026-06-02T21:05:00Z"),
                    source = EventSource.parse("sample-agent"),
                    workItemId = buildReviewId,
                    payload = WorkBlockedPayload(
                        reason = WorkSummary.parse("CI failed on the core test task."),
                    ),
                ),
            ),
        )
    }
}

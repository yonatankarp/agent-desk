package com.yonatankarp.agentdesk.desktop

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

object SampleDesktopState {
    fun current(): DesktopOperatorState {
        val shellId = WorkItemId.parse("agent-task:45")
        val decisionId = WorkItemId.parse("agent-task:46")
        val blockedId = WorkItemId.parse("agent-task:47")

        return DesktopOperatorState(
            workItems = listOf(
                WorkItem(
                    id = shellId,
                    title = WorkItemTitle.parse("Scaffold desktop shell"),
                    status = WorkStatus.Running,
                    summary = WorkSummary.parse("Compose app is rendering sample operational state."),
                ),
                WorkItem(
                    id = decisionId,
                    title = WorkItemTitle.parse("Confirm review boundary"),
                    status = WorkStatus.NeedsDecision,
                    summary = WorkSummary.parse("Operator decision required before adapter review expands."),
                ),
                WorkItem(
                    id = blockedId,
                    title = WorkItemTitle.parse("Resolve failing build"),
                    status = WorkStatus.Blocked,
                    summary = WorkSummary.parse("CI failed on the desktop build task."),
                ),
            ),
            events = listOf(
                WorkEvent(
                    id = WorkEventId.parse("event:agent-task:45:started"),
                    occurredAt = EventTimestamp.parse("2026-06-02T22:45:00Z"),
                    source = EventSource.parse("sample-agent"),
                    workItemId = shellId,
                    payload = WorkStartedPayload(
                        title = WorkItemTitle.parse("Scaffold desktop shell"),
                        summary = WorkSummary.parse("Compose app is rendering sample operational state."),
                    ),
                ),
                WorkEvent(
                    id = WorkEventId.parse("event:agent-task:47:blocked"),
                    occurredAt = EventTimestamp.parse("2026-06-02T22:50:00Z"),
                    source = EventSource.parse("sample-agent"),
                    workItemId = blockedId,
                    payload = WorkBlockedPayload(
                        reason = WorkSummary.parse("CI failed on the desktop build task."),
                    ),
                ),
            ),
        )
    }
}

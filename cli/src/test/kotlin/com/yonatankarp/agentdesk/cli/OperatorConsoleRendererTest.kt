package com.yonatankarp.agentdesk.cli

import com.yonatankarp.agentdesk.app.operator.OperatorState
import com.yonatankarp.agentdesk.app.operator.SampleOperatorState
import com.yonatankarp.agentdesk.core.domain.entities.WorkItem
import com.yonatankarp.agentdesk.core.domain.events.EventSource
import com.yonatankarp.agentdesk.core.domain.events.EventTimestamp
import com.yonatankarp.agentdesk.core.domain.events.EvidenceLabel
import com.yonatankarp.agentdesk.core.domain.events.EvidenceReference
import com.yonatankarp.agentdesk.core.domain.events.EvidenceReferenceKind
import com.yonatankarp.agentdesk.core.domain.events.EvidenceTarget
import com.yonatankarp.agentdesk.core.domain.events.WorkEvent
import com.yonatankarp.agentdesk.core.domain.events.WorkEventId
import com.yonatankarp.agentdesk.core.domain.events.WorkStartedPayload
import com.yonatankarp.agentdesk.core.domain.projections.StaleWorkAttention
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemId
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemTitle
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkStatus
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkSummary
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class OperatorConsoleRendererTest {
    private val renderer = OperatorConsoleRenderer()

    @Test
    fun `renders current work, attention queue, and event timeline`() {
        val workItemId = WorkItemId.parse("agent-task:42")
        val staleWorkItemId = WorkItemId.parse("agent-task:45")
        val output = renderer.render(
            OperatorState(
                workItems = listOf(
                    WorkItem(
                        id = workItemId,
                        title = WorkItemTitle.parse("Run public hygiene check"),
                        status = WorkStatus.NeedsDecision,
                        summary = WorkSummary.parse("Operator decision needed before continuing."),
                    ),
                    WorkItem(
                        id = staleWorkItemId,
                        title = WorkItemTitle.parse("Watch long-running import"),
                        status = WorkStatus.Running,
                    ),
                ),
                events = listOf(
                    WorkEvent(
                        id = WorkEventId.parse("event:agent-task:42:started"),
                        occurredAt = EventTimestamp.parse("2026-06-02T21:00:00Z"),
                        source = EventSource.parse("sample-agent"),
                        workItemId = workItemId,
                        payload = WorkStartedPayload(
                            title = WorkItemTitle.parse("Run public hygiene check"),
                            summary = WorkSummary.parse("Agent accepted the task and started checks."),
                        ),
                        evidenceReferences = listOf(
                            EvidenceReference(
                                kind = EvidenceReferenceKind.Commit,
                                label = EvidenceLabel.parse("Implementation commit"),
                                target = EvidenceTarget.parse("commit:80de32988617392e1f42e6c4c48c66a56aaae4c4"),
                            ),
                        ),
                    ),
                ),
                staleAttention = listOf(
                    StaleWorkAttention(
                        workItemId = staleWorkItemId,
                        status = WorkStatus.Running,
                        lastEventAt = EventTimestamp.parse("2026-06-02T21:00:00Z"),
                        staleForMinutes = 90,
                    ),
                ),
            ),
        )

        assertContains(output, "Current work")
        assertContains(output, "- [Needs decision] agent-task:42 Run public hygiene check")
        assertContains(output, "Attention queue")
        assertContains(output, "- agent-task:42 Run public hygiene check (Needs decision)")
        assertContains(output, "- agent-task:45 Watch long-running import (Stale Running, last event 90m before latest event)")
        assertContains(
            output,
            "- 2026-06-02T21:00:00Z work.started agent-task:42 from sample-agent - " +
                "Agent accepted the task and started checks. | evidence: " +
                "commit Implementation commit -> commit:80de32988617392e1f42e6c4c48c66a56aaae4c4",
        )
    }

    @Test
    fun `renders empty sections explicitly`() {
        val output = renderer.render(OperatorState(workItems = emptyList(), events = emptyList()))

        assertEquals(
            """
            Agent Desk

            Current work
            - none

            Attention queue
            - none

            Recent events
            - none
            """.trimIndent(),
            output,
        )
    }

    @Test
    fun `sample output stays public safe and adapter neutral`() {
        val output = renderer.render(SampleOperatorState.current())

        assertContains(output, "sample-agent")
        assertFalse(output.contains("/home/"))
        assertFalse(output.contains("discord", ignoreCase = true))
        assertFalse(output.contains("token", ignoreCase = true))
        assertFalse(output.contains("op://", ignoreCase = true))
    }
}

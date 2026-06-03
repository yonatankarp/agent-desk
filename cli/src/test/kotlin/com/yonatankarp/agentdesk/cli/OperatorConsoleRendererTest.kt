package com.yonatankarp.agentdesk.cli

import com.yonatankarp.agentdesk.app.operator.OperatorState
import com.yonatankarp.agentdesk.app.operator.SampleOperatorState
import com.yonatankarp.agentdesk.core.domain.entities.WorkItem
import com.yonatankarp.agentdesk.core.domain.events.EventSource
import com.yonatankarp.agentdesk.core.domain.events.EventTimestamp
import com.yonatankarp.agentdesk.core.domain.events.WorkEvent
import com.yonatankarp.agentdesk.core.domain.events.WorkEventId
import com.yonatankarp.agentdesk.core.domain.events.WorkStartedPayload
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemId
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemTitle
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkStatus
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkSummary
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlin.test.Test

class OperatorConsoleRendererTest {
    private val renderer = OperatorConsoleRenderer()

    @Test
    fun `renders current work, attention queue, and event timeline`() {
        val workItemId = WorkItemId.parse("agent-task:42")
        val output = renderer.render(
            OperatorState(
                workItems = listOf(
                    WorkItem(
                        id = workItemId,
                        title = WorkItemTitle.parse("Run public hygiene check"),
                        status = WorkStatus.NeedsDecision,
                        summary = WorkSummary.parse("Operator decision needed before continuing."),
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
                    ),
                ),
            ),
        )

        output shouldContain "Current work"
        output shouldContain "- [NeedsDecision] agent-task:42 Run public hygiene check"
        output shouldContain "Attention queue"
        output shouldContain "- agent-task:42 Run public hygiene check (NeedsDecision)"
        output shouldContain (
            "- 2026-06-02T21:00:00Z work.started agent-task:42 from sample-agent - " +
                "Agent accepted the task and started checks."
            )
    }

    @Test
    fun `renders empty sections explicitly`() {
        val output = renderer.render(OperatorState(workItems = emptyList(), events = emptyList()))

        output shouldBe
            """
            Agent Desk

            Current work
            - none

            Attention queue
            - none

            Recent events
            - none
            """.trimIndent()
    }

    @Test
    fun `sample output stays public safe and adapter neutral`() {
        val output = renderer.render(SampleOperatorState.current())

        output shouldContain "sample-agent"
        output shouldNotContain "/home/"
        output.lowercase() shouldNotContain "discord"
        output.lowercase() shouldNotContain "token"
        output.lowercase() shouldNotContain "op://"
    }
}

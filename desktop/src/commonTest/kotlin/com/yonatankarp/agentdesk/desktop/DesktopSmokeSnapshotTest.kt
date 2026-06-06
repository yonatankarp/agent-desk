package com.yonatankarp.agentdesk.desktop

import com.yonatankarp.agentdesk.app.operator.OperatorState
import com.yonatankarp.agentdesk.app.operator.SampleOperatorState
import com.yonatankarp.agentdesk.core.domain.entities.WorkItem
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemId
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemTitle
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkStatus
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkSummary
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class DesktopSmokeSnapshotTest {
    @Test
    fun `sample state exposes expected desktop sections`() {
        val snapshot = DesktopSmokeSnapshotBuilder.from(SampleOperatorState.current())
        val text = snapshot.flattenedText()

        assertContains(text, "Agent Desk")
        assertContains(text, "Sample state")
        assertContains(text, "Replay status")
        assertContains(text, "Work state")
        assertContains(text, "Read-only timeline")
        assertContains(text, "Decision queue")
        assertContains(text, "Not done: 2 item(s) need operator attention.")
        assertContains(text, "Discovery/no-issue output is triage, not product completion.")
        assertContains(text, "[Running] agent-task:42 Run public hygiene check")
        assertContains(text, "work.started agent-task:42 from sample-agent")
        assertContains(text, "[Needs decision] agent-task:43 Choose adapter boundary")
        assertContains(text, "[Blocked] agent-task:44 Review build failure")
    }

    @Test
    fun `empty state exposes explicit empty rows`() {
        val snapshot =
            DesktopSmokeSnapshotBuilder.from(
                DesktopScreenState.Ready(
                    state = OperatorState(workItems = emptyList(), events = emptyList()),
                    modeLabel = "Loaded state",
                ),
            )

        assertEquals("Loaded state", snapshot.modeLabel)
        assertEquals("0 active / 0 attention", snapshot.summary)
        assertContains(
            snapshot.sectionRows("Replay status").joinToString("\n"),
            "Empty queue: no current work or decisions; not product completion without milestone evidence.",
        )
        assertEquals(listOf("No current work"), snapshot.sectionRows("Work state"))
        assertEquals(listOf("No recent events"), snapshot.sectionRows("Read-only timeline"))
        assertEquals(listOf("No items need a decision"), snapshot.sectionRows("Decision queue"))
    }

    @Test
    fun `attention needed state appears in attention queue`() {
        val item =
            WorkItem(
                id = WorkItemId.parse("agent-task:91"),
                title = WorkItemTitle.parse("Choose retry path"),
                status = WorkStatus.NeedsDecision,
                summary = WorkSummary.parse("Operator must choose whether to retry."),
            )

        val snapshot =
            DesktopSmokeSnapshotBuilder.from(
                DesktopScreenState.Ready(
                    state = OperatorState(workItems = listOf(item), events = emptyList()),
                    modeLabel = "Loaded state",
                ),
            )

        assertEquals("1 active / 1 attention", snapshot.summary)
        assertEquals(
            listOf("[Needs decision] agent-task:91 Choose retry path - Operator must choose whether to retry."),
            snapshot.sectionRows("Decision queue"),
        )
    }

    @Test
    fun `sample snapshot remains public safe`() {
        val text = DesktopSmokeSnapshotBuilder.from(SampleOperatorState.current()).flattenedText()

        assertFalse(text.contains("/" + "home/"))
        assertFalse(text.contains("discord", ignoreCase = true))
        assertFalse(text.contains("token", ignoreCase = true))
        assertFalse(text.contains("op://", ignoreCase = true))
        assertFalse(text.contains("raw transcript", ignoreCase = true))
    }

    private fun DesktopSmokeSnapshot.sectionRows(title: String): List<String> = sections.single { it.title == title }.rows
}

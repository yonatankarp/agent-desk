package dev.agentdesk.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class WorkItemTest {
    @Test
    fun titleAndSummaryNormalizeWhitespace() {
        val title = WorkItemTitle.parse("  Review   build logs  ")
        val summary = WorkSummary.parse("  CI failed   on the core test task.  ")

        assertEquals("Review build logs", title.value)
        assertEquals("CI failed on the core test task.", summary.value)
    }

    @Test
    fun rejectsBlankTitleAndSummary() {
        assertFailsWith<IllegalArgumentException> {
            WorkItemTitle.parse("   ")
        }
        assertFailsWith<IllegalArgumentException> {
            WorkSummary.parse("   ")
        }
    }

    @Test
    fun rejectsMultilineTitleAndSummary() {
        assertFailsWith<IllegalArgumentException> {
            WorkItemTitle.parse("Review logs\nand retry")
        }
        assertFailsWith<IllegalArgumentException> {
            WorkSummary.parse("Build failed\nSee private log path")
        }
    }

    @Test
    fun appliesValidTransition() {
        val item = WorkItem(
            id = WorkItemId.parse("agent-task:42"),
            title = WorkItemTitle.parse("Run public hygiene check"),
            status = WorkStatus.Queued,
        )

        val started = item.transitionTo(WorkStatus.Running)

        assertEquals(WorkStatus.Running, started.status)
        assertEquals(WorkStatus.Queued, item.status)
    }

    @Test
    fun rejectsInvalidTransition() {
        val item = WorkItem(
            id = WorkItemId.parse("agent-task:42"),
            title = WorkItemTitle.parse("Run public hygiene check"),
            status = WorkStatus.Succeeded,
        )

        assertFailsWith<IllegalArgumentException> {
            item.transitionTo(WorkStatus.Running)
        }
    }
}

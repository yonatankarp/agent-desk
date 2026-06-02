package com.yonatankarp.agentdesk.desktop

import com.yonatankarp.agentdesk.core.WorkStatus
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class DesktopStatePresenterTest {
    @Test
    fun `sample state exposes active and attention counts`() {
        val state = SampleDesktopState.current()

        assertEquals(3, DesktopStatePresenter.activeCount(state))
        assertEquals(
            listOf("agent-task:46", "agent-task:47"),
            DesktopStatePresenter.attentionItems(state).map { it.id.toString() },
        )
    }

    @Test
    fun `event lines are adapter neutral and public safe`() {
        val lines = DesktopStatePresenter.eventLines(SampleDesktopState.current())
        val text = lines.joinToString("\n") { line ->
            "${line.occurredAt} ${line.type} ${line.workItemId} ${line.source} ${line.detail}"
        }

        assertContains(text, "sample-agent")
        assertContains(text, "work.started")
        assertContains(text, "work.blocked")
        assertFalse(text.contains("/home/"))
        assertFalse(text.contains("discord", ignoreCase = true))
        assertFalse(text.contains("token", ignoreCase = true))
        assertFalse(text.contains("op://", ignoreCase = true))
    }

    @Test
    fun `status presentation separates lifecycle labels from tones`() {
        assertEquals(
            StatusPresentation("Needs decision", StatusTone.Attention),
            DesktopStatePresenter.presentationFor(WorkStatus.NeedsDecision),
        )
        assertEquals(
            StatusPresentation("Blocked", StatusTone.Blocked),
            DesktopStatePresenter.presentationFor(WorkStatus.Blocked),
        )
    }
}

package com.yonatankarp.agentdesk.cli

import com.yonatankarp.agentdesk.app.operator.OperatorState
import com.yonatankarp.agentdesk.app.operator.OperatorStatePresenter
import com.yonatankarp.agentdesk.core.domain.entities.WorkItem

class OperatorConsoleRenderer {
    fun render(state: OperatorState): String = buildString {
        appendLine("Agent Desk")
        appendLine()
        appendWorkItems(state.workItems)
        appendLine()
        appendAttentionQueue(state)
        appendLine()
        appendRecentEvents(state)
    }.trimEnd()

    private fun StringBuilder.appendWorkItems(workItems: List<WorkItem>) {
        appendLine("Current work")
        if (workItems.isEmpty()) {
            appendLine("- none")
            return
        }

        workItems.forEach { item ->
            appendLine("- [${item.status}] ${item.id} ${item.title}")
            item.summary?.let { summary ->
                appendLine("  $summary")
            }
        }
    }

    private fun StringBuilder.appendAttentionQueue(state: OperatorState) {
        appendLine("Attention queue")
        val attentionItems = OperatorStatePresenter.attentionItems(state)
        if (attentionItems.isEmpty()) {
            appendLine("- none")
            return
        }

        attentionItems.forEach { item ->
            appendLine("- ${item.id} ${item.title} (${item.status})")
        }
    }

    private fun StringBuilder.appendRecentEvents(state: OperatorState) {
        appendLine("Recent events")
        val lines = OperatorStatePresenter.eventLines(state)
        if (lines.isEmpty()) {
            appendLine("- none")
            return
        }

        lines.forEach { line ->
            appendLine(
                "- ${line.occurredAt} ${line.type} ${line.workItemId} " +
                    "from ${line.source} - ${line.detail}",
            )
        }
    }
}

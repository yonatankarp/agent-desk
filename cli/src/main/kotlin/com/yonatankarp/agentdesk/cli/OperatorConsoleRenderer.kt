package com.yonatankarp.agentdesk.cli

import com.yonatankarp.agentdesk.core.domain.entities.WorkItem
import com.yonatankarp.agentdesk.core.domain.events.WorkBlockedPayload
import com.yonatankarp.agentdesk.core.domain.events.WorkEvent
import com.yonatankarp.agentdesk.core.domain.events.WorkEventPayload
import com.yonatankarp.agentdesk.core.domain.events.WorkStartedPayload

class OperatorConsoleRenderer {
    fun render(state: OperatorState): String = buildString {
        appendLine("Agent Desk")
        appendLine()
        appendWorkItems(state.workItems)
        appendLine()
        appendAttentionQueue(state.workItems)
        appendLine()
        appendRecentEvents(state.events)
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

    private fun StringBuilder.appendAttentionQueue(workItems: List<WorkItem>) {
        appendLine("Attention queue")
        val attentionItems = workItems.filter { it.status.requiresHumanAttention }
        if (attentionItems.isEmpty()) {
            appendLine("- none")
            return
        }

        attentionItems.forEach { item ->
            appendLine("- ${item.id} ${item.title} (${item.status})")
        }
    }

    private fun StringBuilder.appendRecentEvents(events: List<WorkEvent>) {
        appendLine("Recent events")
        if (events.isEmpty()) {
            appendLine("- none")
            return
        }

        events.forEach { event ->
            appendLine(
                "- ${event.occurredAt} ${event.type.wireName} ${event.workItemId} " +
                    "from ${event.source} - ${event.payload.describe()}",
            )
        }
    }

    private fun WorkEventPayload.describe(): String = when (this) {
        is WorkStartedPayload -> summary?.toString() ?: title.toString()
        is WorkBlockedPayload -> reason.toString()
    }
}

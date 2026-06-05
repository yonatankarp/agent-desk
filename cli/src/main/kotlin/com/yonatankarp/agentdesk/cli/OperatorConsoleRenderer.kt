package com.yonatankarp.agentdesk.cli

import com.yonatankarp.agentdesk.app.operator.EventLine
import com.yonatankarp.agentdesk.app.operator.OperatorState
import com.yonatankarp.agentdesk.app.operator.OperatorStatePresenter
import com.yonatankarp.agentdesk.app.operator.WorkItemInspection
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

    fun render(inspection: WorkItemInspection): String = buildString {
        appendLine("Agent Desk")
        appendLine()
        appendLine("Work item ${inspection.item.id}")
        appendLine("Status: ${inspection.statusPresentation.label}")
        appendLine("Title: ${inspection.item.title}")
        appendLine("Summary: ${inspection.item.summary ?: "none"}")
        appendLine("Attention: ${inspection.requiresAttention.toYesNo()}")
        appendLine("Terminal: ${inspection.isTerminal.toYesNo()}")
        appendLine()
        appendAcceptedEvents(inspection)
        appendLine()
        appendProjectionWarnings(inspection)
        appendLine()
        appendLine("Evidence references")
        appendLine("- none")
    }.trimEnd()

    private fun StringBuilder.appendWorkItems(workItems: List<WorkItem>) {
        appendLine("Current work")
        if (workItems.isEmpty()) {
            appendLine("- none")
            return
        }

        workItems.forEach { item ->
            appendLine("- [${OperatorStatePresenter.presentationFor(item.status).label}] ${item.id} ${item.title}")
            item.summary?.let { summary ->
                appendLine("  $summary")
            }
        }
    }

    private fun StringBuilder.appendAttentionQueue(state: OperatorState) {
        appendLine("Attention queue")
        val attentionItems = OperatorStatePresenter.attentionItems(state)
        val staleItems = OperatorStatePresenter.staleAttentionItems(state)
        if (attentionItems.isEmpty() && staleItems.isEmpty()) {
            appendLine("- none")
            return
        }

        attentionItems.forEach { item ->
            appendLine("- ${item.id} ${item.title} (${OperatorStatePresenter.presentationFor(item.status).label})")
        }
        staleItems.forEach { stale ->
            val item = state.workItems.firstOrNull { it.id == stale.workItemId }
            appendLine(
                "- ${stale.workItemId} ${item?.title ?: "Unknown work"} " +
                    "(Stale ${OperatorStatePresenter.presentationFor(stale.status).label}, last event ${stale.staleForMinutes}m before latest event)",
            )
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
            appendLine("- ${line.occurredAt} ${line.type} ${line.workItemId} from ${line.source} - ${line.describe()}")
        }
    }

    private fun StringBuilder.appendAcceptedEvents(inspection: WorkItemInspection) {
        appendLine("Accepted recent events")
        if (inspection.acceptedEvents.isEmpty()) {
            appendLine("- none")
            return
        }

        inspection.acceptedEvents.forEach { line ->
            appendLine("- ${line.occurredAt} ${line.type} from ${line.source} - ${line.describe()}")
        }
    }

    private fun StringBuilder.appendProjectionWarnings(inspection: WorkItemInspection) {
        appendLine("Projection warnings")
        if (inspection.projectionWarnings.isEmpty()) {
            appendLine("- none")
            return
        }

        inspection.projectionWarnings.forEach { warning ->
            appendLine("- ignored event - ${warning.reason}")
        }
    }

    private fun EventLine.describe(): String {
        if (evidenceReferences.isEmpty()) {
            return detail
        }

        val evidence = evidenceReferences.joinToString("; ") { reference ->
            "${reference.kind} ${reference.label} -> ${reference.target}"
        }
        return "$detail | evidence: $evidence"
    }

    private fun Boolean.toYesNo(): String = if (this) "yes" else "no"
}

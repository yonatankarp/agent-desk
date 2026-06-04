package com.yonatankarp.agentdesk.desktop

import com.yonatankarp.agentdesk.app.operator.OperatorState
import com.yonatankarp.agentdesk.app.operator.OperatorStatePresenter
import com.yonatankarp.agentdesk.core.domain.entities.WorkItem

data class DesktopSmokeSnapshot(
    val title: String,
    val modeLabel: String,
    val summary: String,
    val sections: List<DesktopSmokeSection>,
) {
    fun flattenedText(): String = buildString {
        appendLine(title)
        appendLine(modeLabel)
        appendLine(summary)
        sections.forEach { section ->
            appendLine(section.title)
            section.rows.forEach(::appendLine)
        }
    }
}

data class DesktopSmokeSection(
    val title: String,
    val rows: List<String>,
)

object DesktopSmokeSnapshotBuilder {
    fun from(state: OperatorState): DesktopSmokeSnapshot {
        val attentionItems = OperatorStatePresenter.attentionItems(state)
        return DesktopSmokeSnapshot(
            title = "Agent Desk",
            modeLabel = "Sample state",
            summary = "${OperatorStatePresenter.activeCount(state)} active / ${attentionItems.size} attention",
            sections = listOf(
                DesktopSmokeSection(
                    title = "Current work",
                    rows = state.workItems.toWorkRows(emptyText = "No current work"),
                ),
                DesktopSmokeSection(
                    title = "Recent events",
                    rows = OperatorStatePresenter.eventLines(state).map { line ->
                        "${line.type} ${line.workItemId} from ${line.source} - ${line.detail}"
                    }.ifEmpty { listOf("No recent events") },
                ),
                DesktopSmokeSection(
                    title = "Attention queue",
                    rows = attentionItems.toWorkRows(emptyText = "No items need a decision"),
                ),
            ),
        )
    }

    private fun List<WorkItem>.toWorkRows(emptyText: String): List<String> {
        if (isEmpty()) {
            return listOf(emptyText)
        }

        return map { item ->
            buildString {
                append("[${item.status}] ${item.id} ${item.title}")
                item.summary?.let { summary ->
                    append(" - $summary")
                }
            }
        }
    }
}

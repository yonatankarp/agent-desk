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
    fun from(state: OperatorState): DesktopSmokeSnapshot = from(DesktopScreenState.Ready(state, modeLabel = "Sample state"))

    fun from(screenState: DesktopScreenState): DesktopSmokeSnapshot {
        val state = (screenState as? DesktopScreenState.Ready)?.state
        val message = screenState.message()
        val workItems = state?.workItems.orEmpty()
        val events = state?.let(OperatorStatePresenter::eventLines).orEmpty()
        val attentionItems = state?.let(OperatorStatePresenter::attentionItems).orEmpty()
        return DesktopSmokeSnapshot(
            title = "Agent Desk",
            modeLabel = screenState.modeLabel,
            summary = screenState.summaryText(),
            sections = listOf(
                DesktopSmokeSection(
                    title = "Replay status",
                    rows = DesktopReplayStatus.rows(screenState),
                ),
                DesktopSmokeSection(
                    title = "Work state",
                    rows = workItems.toWorkRows(emptyText = "No current work"),
                ),
                DesktopSmokeSection(
                    title = "Read-only timeline",
                    rows = events.map { line ->
                        "${line.type} ${line.workItemId} from ${line.source} - ${line.detail}"
                    }.ifEmpty { listOf("No recent events") },
                ),
                DesktopSmokeSection(
                    title = "Decision queue",
                    rows = message?.let(::listOf) ?: attentionItems.toWorkRows(emptyText = "No items need a decision"),
                ),
            ),
        )
    }

    private fun DesktopScreenState.summaryText(): String {
        val state = (this as? DesktopScreenState.Ready)?.state ?: return "0 active / 0 attention"
        val attentionItems = OperatorStatePresenter.attentionItems(state)
        return "${OperatorStatePresenter.activeCount(state)} active / ${attentionItems.size} attention"
    }

    private fun DesktopScreenState.message(): String? = when (this) {
        DesktopScreenState.Loading -> "Loading operator state"
        is DesktopScreenState.Error -> message
        is DesktopScreenState.Ready -> null
    }

    private fun List<WorkItem>.toWorkRows(emptyText: String): List<String> {
        if (isEmpty()) {
            return listOf(emptyText)
        }

        return map { item ->
            buildString {
                append("[${OperatorStatePresenter.presentationFor(item.status).label}] ${item.id} ${item.title}")
                item.summary?.let { summary ->
                    append(" - $summary")
                }
            }
        }
    }
}

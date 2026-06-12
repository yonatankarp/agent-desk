package com.yonatankarp.agentdesk.desktop

import com.yonatankarp.agentdesk.app.operator.OperatorDisplaySection
import com.yonatankarp.agentdesk.app.operator.OperatorDisplayStructure
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
        val timelineRows = DesktopTimelinePresenter.snapshotRows(state)
        val attentionItems = state?.let(OperatorStatePresenter::attentionItems).orEmpty()
        return DesktopSmokeSnapshot(
            title = "Agent Desk",
            modeLabel = screenState.modeLabel,
            summary = screenState.summaryText(),
            sections = OperatorDisplayStructure.orderedSections.map { section ->
                DesktopSmokeSection(
                    title = section.desktopLabel,
                    rows = section.rows(
                        screenState = screenState,
                        workItems = workItems,
                        timelineRows = timelineRows,
                        attentionItems = attentionItems,
                        message = message,
                    ),
                )
            },
        )
    }

    private fun OperatorDisplaySection.rows(
        screenState: DesktopScreenState,
        workItems: List<WorkItem>,
        timelineRows: List<String>,
        attentionItems: List<WorkItem>,
        message: String?,
    ): List<String> = when (this) {
        OperatorDisplaySection.ReplayStatus -> DesktopReplayStatus.rows(screenState)

        OperatorDisplaySection.WorkState -> workItems.toWorkRows(emptyText = "No current work")

        OperatorDisplaySection.Timeline -> timelineRows

        OperatorDisplaySection.DecisionQueue -> message?.let(::listOf) ?: attentionItems.toWorkRows(
            emptyText = "No items need a decision",
        )

        OperatorDisplaySection.EvidenceDetail -> DesktopEvidenceDrilldown.rows(screenState)
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

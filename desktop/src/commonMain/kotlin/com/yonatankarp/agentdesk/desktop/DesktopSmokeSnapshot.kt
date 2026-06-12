package com.yonatankarp.agentdesk.desktop

import com.yonatankarp.agentdesk.app.operator.OperatorDisplaySection
import com.yonatankarp.agentdesk.app.operator.OperatorDisplayStructure
import com.yonatankarp.agentdesk.app.operator.OperatorState
import com.yonatankarp.agentdesk.app.operator.OperatorStatePresenter

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
        val workRows = DesktopWorkItemPresenter.snapshotRows(
            state,
            workItems,
            emptyText = "No current work",
            includeStale = false,
        )
        val timelineRows = DesktopTimelinePresenter.snapshotRows(state)
        val attentionItems = DesktopWorkItemPresenter.attentionItems(state)
        val attentionRows = DesktopWorkItemPresenter.snapshotRows(
            state,
            attentionItems,
            emptyText = "No items need a decision",
            includeStale = true,
        )
        return DesktopSmokeSnapshot(
            title = "Agent Desk",
            modeLabel = screenState.modeLabel,
            summary = screenState.summaryText(),
            sections = OperatorDisplayStructure.orderedSections.map { section ->
                DesktopSmokeSection(
                    title = section.desktopLabel,
                    rows = section.rows(
                        screenState = screenState,
                        workRows = workRows,
                        timelineRows = timelineRows,
                        attentionRows = attentionRows,
                        message = message,
                    ),
                )
            },
        )
    }

    private fun OperatorDisplaySection.rows(
        screenState: DesktopScreenState,
        workRows: List<String>,
        timelineRows: List<String>,
        attentionRows: List<String>,
        message: String?,
    ): List<String> = when (this) {
        OperatorDisplaySection.ReplayStatus -> DesktopReplayStatus.rows(screenState)
        OperatorDisplaySection.WorkState -> workRows
        OperatorDisplaySection.Timeline -> timelineRows
        OperatorDisplaySection.DecisionQueue -> message?.let(::listOf) ?: attentionRows
        OperatorDisplaySection.EvidenceDetail -> DesktopEvidenceDrilldown.rows(screenState)
    }

    private fun DesktopScreenState.summaryText(): String {
        val state = (this as? DesktopScreenState.Ready)?.state ?: return "0 active / 0 attention"
        val attentionItems = DesktopWorkItemPresenter.attentionItems(state)
        return "${OperatorStatePresenter.activeCount(state)} active / ${attentionItems.size} attention"
    }

    private fun DesktopScreenState.message(): String? = when (this) {
        DesktopScreenState.Loading -> "Loading operator state"
        is DesktopScreenState.Error -> message
        is DesktopScreenState.Ready -> null
    }
}

package com.yonatankarp.agentdesk.mobile

import com.yonatankarp.agentdesk.app.operator.OperatorDisplaySection
import com.yonatankarp.agentdesk.app.operator.OperatorDisplayStructure
import com.yonatankarp.agentdesk.app.operator.mobile.MobileAttentionItem
import com.yonatankarp.agentdesk.app.operator.mobile.MobileEventLine
import com.yonatankarp.agentdesk.app.operator.mobile.MobileOperatorState
import com.yonatankarp.agentdesk.app.operator.mobile.MobileOperatorStateContract
import com.yonatankarp.agentdesk.app.operator.mobile.MobileProjectionWarning
import com.yonatankarp.agentdesk.app.operator.mobile.MobileTimelineEntry
import com.yonatankarp.agentdesk.app.operator.mobile.MobileWorkItem

data class MobileSmokeSnapshot(
    val title: String,
    val summary: String,
    val sections: List<MobileSmokeSection>,
) {
    fun flattenedText(): String = buildString {
        appendLine(title)
        appendLine(summary)
        sections.forEach { section ->
            appendLine(section.title)
            section.rows.forEach(::appendLine)
        }
    }
}

data class MobileSmokeSection(
    val title: String,
    val rows: List<String>,
)

object MobileSmokeSnapshotBuilder {
    fun sample(): MobileSmokeSnapshot = from(MobileOperatorStateContract.sample())

    fun from(state: MobileOperatorState): MobileSmokeSnapshot = MobileSmokeSnapshot(
        title = MobileDisplayText.APP_TITLE,
        summary = MobileDisplayText.summary(
            currentWorkCount = state.currentWork.size,
            attentionCount = state.attentionQueue.size,
        ),
        sections = OperatorDisplayStructure.orderedSections.map { section ->
            MobileSmokeSection(
                title = section.mobileLabel,
                rows = section.rows(state),
            )
        },
    )

    private fun OperatorDisplaySection.rows(state: MobileOperatorState): List<String> = when (this) {
        OperatorDisplaySection.ReplayStatus -> state.projectionWarnings.toWarningRows()

        OperatorDisplaySection.WorkState -> state.currentWork.toWorkRows(emptyText = MobileDisplayText.NO_CURRENT_WORK)

        OperatorDisplaySection.Timeline -> state.recentEvents.toEventRows() + state.timeline.toTimelineRows(
            state.timelineStatusMarkers,
        )

        OperatorDisplaySection.DecisionQueue -> state.attentionQueue.toAttentionRows()

        OperatorDisplaySection.EvidenceDetail -> state.evidenceDetails.firstOrNull()
            ?.let(MobileDisplayText::evidenceDetailRows)
            ?: listOf(MobileDisplayText.EVIDENCE_DETAIL_MISSING)
    }

    private fun List<MobileWorkItem>.toWorkRows(emptyText: String): List<String> {
        if (isEmpty()) {
            return listOf(emptyText)
        }

        return map { item -> item.describe() }
    }

    private fun List<MobileAttentionItem>.toAttentionRows(): List<String> {
        if (isEmpty()) {
            return listOf(MobileDisplayText.NO_ITEMS_NEED_ATTENTION)
        }

        return map { attention ->
            buildString {
                append(attention.workItem.describe())
                attention.stale?.let { stale ->
                    append(" (${MobileDisplayText.staleAttention(stale)})")
                }
            }
        }
    }

    private fun List<MobileEventLine>.toEventRows(): List<String> {
        if (isEmpty()) {
            return listOf(MobileDisplayText.NO_RECENT_ACCEPTED_EVENTS)
        }

        return map { event -> event.describe() }
    }

    private fun List<MobileTimelineEntry>.toTimelineRows(markers: List<String>): List<String> {
        if (isEmpty()) {
            return listOf(MobileDisplayText.NO_TIMELINE_ENTRIES)
        }

        return listOf(MobileDisplayText.timelineStatus(markers)) + map { entry -> MobileDisplayText.timelineRow(entry) }
    }

    private fun List<MobileProjectionWarning>.toWarningRows(): List<String> {
        if (isEmpty()) {
            return listOf(MobileDisplayText.NO_PROJECTION_WARNINGS)
        }

        return map { warning -> "${warning.eventId} - ${warning.reason}" }
    }

    private fun MobileWorkItem.describe(): String = MobileDisplayText.workRow(this)

    private fun MobileEventLine.describe(): String = MobileDisplayText.eventRow(this)
}

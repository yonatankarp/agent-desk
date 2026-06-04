package com.yonatankarp.agentdesk.mobile

import com.yonatankarp.agentdesk.app.operator.mobile.MobileAttentionItem
import com.yonatankarp.agentdesk.app.operator.mobile.MobileEventLine
import com.yonatankarp.agentdesk.app.operator.mobile.MobileOperatorState
import com.yonatankarp.agentdesk.app.operator.mobile.MobileOperatorStateContract
import com.yonatankarp.agentdesk.app.operator.mobile.MobileProjectionWarning
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
        title = "Agent Desk",
        summary = "${state.currentWork.size} current / ${state.attentionQueue.size} attention",
        sections = buildList {
            add(
                MobileSmokeSection(
                    title = "Current work",
                    rows = state.currentWork.toWorkRows(emptyText = "No current work"),
                ),
            )
            add(
                MobileSmokeSection(
                    title = "Attention queue",
                    rows = state.attentionQueue.toAttentionRows(),
                ),
            )
            add(
                MobileSmokeSection(
                    title = "Recent events",
                    rows = state.recentEvents.toEventRows(),
                ),
            )
            if (state.projectionWarnings.isNotEmpty()) {
                add(
                    MobileSmokeSection(
                        title = "Projection warnings",
                        rows = state.projectionWarnings.toWarningRows(),
                    ),
                )
            }
        },
    )

    private fun List<MobileWorkItem>.toWorkRows(emptyText: String): List<String> {
        if (isEmpty()) {
            return listOf(emptyText)
        }

        return map { item -> item.describe() }
    }

    private fun List<MobileAttentionItem>.toAttentionRows(): List<String> {
        if (isEmpty()) {
            return listOf("No items need attention")
        }

        return map { attention ->
            buildString {
                append(attention.workItem.describe())
                attention.stale?.let { stale ->
                    append(" (Stale ${stale.staleForMinutes}m since ${stale.lastEventAt})")
                }
            }
        }
    }

    private fun List<MobileEventLine>.toEventRows(): List<String> {
        if (isEmpty()) {
            return listOf("No recent accepted events")
        }

        return map { event -> event.describe() }
    }

    private fun List<MobileProjectionWarning>.toWarningRows(): List<String> = map { warning -> "${warning.eventId} - ${warning.reason}" }

    private fun MobileWorkItem.describe(): String = buildString {
        append("[${status.label}] $id $title")
        summary?.let { append(" - $it") }
        if (evidenceReferences.isNotEmpty()) {
            val evidence = evidenceReferences.joinToString { reference -> "${reference.kind}:${reference.label}" }
            append(" | Evidence: $evidence")
        }
    }

    private fun MobileEventLine.describe(): String = buildString {
        append("$occurredAt [$type] $workItemId - $detail")
        if (evidenceReferences.isNotEmpty()) {
            val evidence = evidenceReferences.joinToString { reference -> "${reference.kind}:${reference.label}" }
            append(" | Evidence: $evidence")
        }
    }
}

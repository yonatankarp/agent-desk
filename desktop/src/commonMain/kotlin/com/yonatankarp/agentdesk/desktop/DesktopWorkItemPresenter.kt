package com.yonatankarp.agentdesk.desktop

import com.yonatankarp.agentdesk.app.operator.EvidenceDisplayFormatter
import com.yonatankarp.agentdesk.app.operator.OperatorState
import com.yonatankarp.agentdesk.app.operator.OperatorStatePresenter
import com.yonatankarp.agentdesk.app.operator.StaleDisplayFormatter
import com.yonatankarp.agentdesk.app.operator.StatusTone
import com.yonatankarp.agentdesk.core.domain.entities.WorkItem

data class DesktopWorkItemRow(
    val id: String,
    val title: String,
    val statusLabel: String,
    val tone: StatusTone,
    val summary: String?,
    val evidenceText: String?,
    val staleText: String?,
) {
    fun snapshotText(): String = buildString {
        append("[$statusLabel] $id $title")
        summary?.let { append(" - $it") }
        evidenceText?.let { append(" | Evidence: $it") }
        staleText?.let { append(" ($it)") }
    }
}

object DesktopWorkItemPresenter {
    fun attentionItems(state: OperatorState?): List<WorkItem> {
        state ?: return emptyList()
        val workItemsById = state.workItems.associateBy { item -> item.id.toString() }
        val staleItems = state.staleAttention.mapNotNull { stale -> workItemsById[stale.workItemId.toString()] }
        return (OperatorStatePresenter.attentionItems(state) + staleItems).distinctBy { item -> item.id.toString() }
    }

    fun rows(
        state: OperatorState?,
        items: List<WorkItem>,
        includeStale: Boolean,
    ): List<DesktopWorkItemRow> {
        val evidenceByWorkItem = state.evidenceByWorkItem()
        val staleByWorkItem = if (includeStale) {
            state?.staleAttention.orEmpty().associateBy { stale -> stale.workItemId.toString() }
        } else {
            emptyMap()
        }
        return items.map { item ->
            val presentation = OperatorStatePresenter.presentationFor(item.status)
            val evidence = evidenceByWorkItem[item.id.toString()].orEmpty().distinct()
            val stale = staleByWorkItem[item.id.toString()]
            DesktopWorkItemRow(
                id = item.id.toString(),
                title = item.title.toString(),
                statusLabel = presentation.label,
                tone = presentation.tone,
                summary = item.summary?.toString(),
                evidenceText = evidence.takeIf { it.isNotEmpty() }?.joinToString(transform = EvidenceDisplayFormatter::format),
                staleText = stale?.let {
                    "Stale ${StaleDisplayFormatter.humanizeMinutes(it.staleForMinutes)} " +
                        "since ${StaleDisplayFormatter.humanizeTimestamp(it.lastEventAt.toString())}"
                },
            )
        }
    }

    fun snapshotRows(
        state: OperatorState?,
        items: List<WorkItem>,
        emptyText: String,
        includeStale: Boolean,
    ): List<String> = rows(state, items, includeStale = includeStale).map(DesktopWorkItemRow::snapshotText).ifEmpty { listOf(emptyText) }

    private fun OperatorState?.evidenceByWorkItem() = this?.let(OperatorStatePresenter::eventLines)
        .orEmpty()
        .groupBy({ line -> line.workItemId }, { line -> line.evidenceReferences })
        .mapValues { (_, references) -> references.flatten() }
}

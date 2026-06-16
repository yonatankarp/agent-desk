package com.yonatankarp.agentdesk.desktop

import com.yonatankarp.agentdesk.app.operator.EvidenceDisplayFormatter
import com.yonatankarp.agentdesk.app.operator.OperatorState
import com.yonatankarp.agentdesk.app.operator.ProvenanceLine
import com.yonatankarp.agentdesk.app.operator.timeline.ReadOnlyTimelineEntry
import com.yonatankarp.agentdesk.app.operator.timeline.ReadOnlyTimelineEntryState
import com.yonatankarp.agentdesk.app.operator.timeline.ReadOnlyTimelineProjector
import com.yonatankarp.agentdesk.app.operator.timeline.ReadOnlyTimelineStateMarker

data class DesktopTimelineRow(
    val type: String,
    val occurredAt: String,
    val detail: String,
    val source: String,
    val timeWindow: String,
)

object DesktopTimelinePresenter {
    fun rows(state: OperatorState?): List<DesktopTimelineRow> {
        val projection = state?.let(ReadOnlyTimelineProjector::project) ?: return emptyList()
        val entries = projection.entries
        if (entries.isEmpty()) {
            return emptyList()
        }

        val statusRow = DesktopTimelineRow(
            type = "Status",
            occurredAt = "",
            detail = projection.stateMarkers.joinToString(", ") { it.label },
            source = "Read-only timeline projection",
            timeWindow = "",
        )
        val entryRows = projection.timeWindowGroups.flatMap { group ->
            val groupEntries = entries.filter { entry -> entry.eventId in group.entryIds }
            groupEntries.map { entry -> entry.toDesktopRow() }
        }

        return listOf(statusRow) + entryRows
    }

    fun snapshotRows(state: OperatorState?): List<String> = rows(state)
        .groupBy { it.timeWindow }
        .flatMap { (timeWindow, rows) ->
            val prefix = if (timeWindow.isBlank()) emptyList() else listOf("Date: $timeWindow")
            prefix + rows.map { it.snapshotText() }
        }
        .ifEmpty { listOf("No recent events") }

    private fun ReadOnlyTimelineEntry.toDesktopRow(): DesktopTimelineRow = DesktopTimelineRow(
        type = type,
        occurredAt = occurredAt,
        detail = buildString {
            append("State: ${state.label}; $summary")
            completionSummary?.let { completion -> append("; Completion: $completion") }
            if (evidenceReferences.isNotEmpty()) {
                append("; Evidence: ")
                append(evidenceReferences.joinToString(" | ", transform = EvidenceDisplayFormatter::format))
            }
            provenance.summary()?.let { summary -> append("; Provenance: $summary") }
        },
        source = "$workItemId from $source",
        timeWindow = timeWindow,
    )

    private fun DesktopTimelineRow.snapshotText(): String = buildString {
        append(type)
        if (occurredAt.isNotBlank()) {
            append(" at $occurredAt")
        }
        append(" - $detail")
        append(" [$source]")
    }

    private fun ProvenanceLine.summary(): String? = listOfNotNull(
        projectId,
        workspaceId,
        sourceId,
        ownerId,
        agentId,
        modelId,
        toolId,
        runId,
        objectiveId,
        parentHandoffId,
        archiveRecordId,
    ).takeIf { it.isNotEmpty() }?.joinToString(" ")

    private val ReadOnlyTimelineEntryState.label: String
        get() = when (this) {
            ReadOnlyTimelineEntryState.ReadOnly -> "Read-only"
            ReadOnlyTimelineEntryState.NotDone -> "Not done"
            ReadOnlyTimelineEntryState.Blocked -> "Blocked"
            ReadOnlyTimelineEntryState.Failed -> "Failed"
            ReadOnlyTimelineEntryState.Completed -> "Completed"
            ReadOnlyTimelineEntryState.Partial -> "Partial"
            ReadOnlyTimelineEntryState.Stale -> "Stale"
        }

    private val ReadOnlyTimelineStateMarker.label: String
        get() = when (this) {
            ReadOnlyTimelineStateMarker.Empty -> "Empty"
            ReadOnlyTimelineStateMarker.ReadOnly -> "Read-only"
            ReadOnlyTimelineStateMarker.NotDone -> "Not done"
            ReadOnlyTimelineStateMarker.Blocked -> "Blocked"
            ReadOnlyTimelineStateMarker.Failed -> "Failed"
            ReadOnlyTimelineStateMarker.Completed -> "Completed"
            ReadOnlyTimelineStateMarker.Partial -> "Partial"
            ReadOnlyTimelineStateMarker.Stale -> "Stale"
        }
}

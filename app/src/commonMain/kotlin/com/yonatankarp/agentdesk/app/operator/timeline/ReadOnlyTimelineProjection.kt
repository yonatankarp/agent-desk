package com.yonatankarp.agentdesk.app.operator.timeline

import com.yonatankarp.agentdesk.app.operator.EvidenceLine
import com.yonatankarp.agentdesk.app.operator.OperatorState
import com.yonatankarp.agentdesk.app.operator.OperatorStatePresenter
import com.yonatankarp.agentdesk.app.operator.ProvenanceLine
import com.yonatankarp.agentdesk.core.domain.events.WorkEventPayload
import com.yonatankarp.agentdesk.core.domain.events.WorkStartedPayload
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkStatus

data class ReadOnlyTimelineProjection(
    val entries: List<ReadOnlyTimelineEntry>,
    val projectGroups: List<ReadOnlyTimelineGroup> = emptyList(),
    val workspaceGroups: List<ReadOnlyTimelineGroup> = emptyList(),
    val sourceGroups: List<ReadOnlyTimelineGroup>,
    val upstreamSourceGroups: List<ReadOnlyTimelineGroup> = emptyList(),
    val ownerGroups: List<ReadOnlyTimelineGroup> = emptyList(),
    val agentGroups: List<ReadOnlyTimelineGroup> = emptyList(),
    val workItemGroups: List<ReadOnlyTimelineGroup>,
    val statusGroups: List<ReadOnlyTimelineGroup> = emptyList(),
    val riskGroups: List<ReadOnlyTimelineGroup> = emptyList(),
    val timeWindowGroups: List<ReadOnlyTimelineGroup>,
    val stateMarkers: List<ReadOnlyTimelineStateMarker>,
)

data class ReadOnlyTimelineEntry(
    val eventId: String,
    val occurredAt: String,
    val timeWindow: String,
    val source: String,
    val workItemId: String,
    val type: String,
    val status: String,
    val state: ReadOnlyTimelineEntryState,
    val summary: String,
    val completionSummary: String?,
    val evidenceReferences: List<EvidenceLine>,
    val diagnosticMarkers: List<String>,
    val provenance: ProvenanceLine = ProvenanceLine(),
)

data class ReadOnlyTimelineGroup(
    val key: String,
    val entryIds: List<String>,
)

enum class ReadOnlyTimelineEntryState {
    ReadOnly,
    NotDone,
    Blocked,
    Failed,
    Completed,
    Partial,
    Stale,
}

enum class ReadOnlyTimelineStateMarker {
    Empty,
    ReadOnly,
    NotDone,
    Blocked,
    Failed,
    Completed,
    Partial,
    Stale,
}

object ReadOnlyTimelineProjector {
    fun project(
        state: OperatorState,
        filter: ReadOnlyTimelineFilter = ReadOnlyTimelineFilter(),
    ): ReadOnlyTimelineProjection {
        val workItemsById = state.workItems.associateBy { it.id.toString() }
        val staleWorkItemIds = state.staleAttention.mapTo(mutableSetOf()) { it.workItemId.toString() }
        val eventLinesById = OperatorStatePresenter.eventLines(state)
            .associateBy { "${it.workItemId}:${it.occurredAt}:${it.type}" }

        val allEntries = state.events.map { event ->
            val workItemId = event.workItemId.toString()
            val workItem = workItemsById[workItemId]
            val status = workItem?.status
            val line = eventLinesById.getValue("$workItemId:${event.occurredAt}:${event.type.wireName}")
            val entryState = status.toEntryState(
                isPartial = workItem == null,
                isStale = workItemId in staleWorkItemIds,
            )

            ReadOnlyTimelineEntry(
                eventId = event.id.toString(),
                occurredAt = event.occurredAt.toString(),
                timeWindow = event.occurredAt.toString().substringBefore("T"),
                source = event.source.toString(),
                workItemId = workItemId,
                type = event.type.wireName,
                status = status?.let(OperatorStatePresenter::presentationFor)?.label ?: "Partial",
                state = entryState,
                summary = line.detail,
                completionSummary = event.payload.completionSummary(),
                evidenceReferences = line.evidenceReferences,
                diagnosticMarkers = listOf("read-only", "import-diagnostics-from-replay"),
                provenance = ProvenanceLine.from(event.provenance) ?: ProvenanceLine(),
            )
        }
        val entries = allEntries.filter(filter::matches)

        return ReadOnlyTimelineProjection(
            entries = entries,
            projectGroups = entries.groupByOptionalKey { it.provenance.projectId },
            workspaceGroups = entries.groupByOptionalKey { it.provenance.workspaceId },
            sourceGroups = entries.groupByKey { it.source },
            upstreamSourceGroups = entries.groupByOptionalKey { it.provenance.sourceId },
            ownerGroups = entries.groupByOptionalKey { it.provenance.ownerId },
            agentGroups = entries.groupByOptionalKey { it.provenance.agentId },
            workItemGroups = entries.groupByKey { it.workItemId },
            statusGroups = entries.groupByKey { it.status },
            riskGroups = entries.groupByKey { it.state.name },
            timeWindowGroups = entries.groupByKey { it.timeWindow },
            stateMarkers = entries.toStateMarkers(),
        )
    }

    private fun WorkStatus?.toEntryState(
        isPartial: Boolean,
        isStale: Boolean,
    ): ReadOnlyTimelineEntryState = when {
        isPartial -> ReadOnlyTimelineEntryState.Partial
        isStale -> ReadOnlyTimelineEntryState.Stale
        this == WorkStatus.Blocked -> ReadOnlyTimelineEntryState.Blocked
        this == WorkStatus.Failed -> ReadOnlyTimelineEntryState.Failed
        this?.isTerminal == true -> ReadOnlyTimelineEntryState.Completed
        this?.requiresHumanAttention == true -> ReadOnlyTimelineEntryState.NotDone
        else -> ReadOnlyTimelineEntryState.ReadOnly
    }

    private fun WorkEventPayload.completionSummary(): String? = when {
        this is WorkStartedPayload -> null
        type.wireName == "work.succeeded" -> "Successful outcome"
        type.wireName == "work.failed" -> "Failed outcome"
        type.wireName == "work.canceled" -> "Canceled outcome"
        else -> null
    }

    private fun List<ReadOnlyTimelineEntry>.groupByKey(
        key: (ReadOnlyTimelineEntry) -> String,
    ): List<ReadOnlyTimelineGroup> = groupBy(key)
        .map { (groupKey, groupEntries) ->
            ReadOnlyTimelineGroup(
                key = groupKey,
                entryIds = groupEntries.map { it.eventId },
            )
        }

    private fun List<ReadOnlyTimelineEntry>.groupByOptionalKey(
        key: (ReadOnlyTimelineEntry) -> String?,
    ): List<ReadOnlyTimelineGroup> = mapNotNull { entry -> key(entry)?.let { it to entry } }
        .groupBy({ it.first }, { it.second })
        .map { (groupKey, groupEntries) ->
            ReadOnlyTimelineGroup(
                key = groupKey,
                entryIds = groupEntries.map { it.eventId },
            )
        }

    private fun List<ReadOnlyTimelineEntry>.toStateMarkers(): List<ReadOnlyTimelineStateMarker> {
        if (isEmpty()) {
            return listOf(ReadOnlyTimelineStateMarker.Empty, ReadOnlyTimelineStateMarker.ReadOnly)
        }

        val entryStates = map { entry -> entry.state }.toSet()
        return buildSet {
            add(ReadOnlyTimelineStateMarker.ReadOnly)
            if (ReadOnlyTimelineEntryState.Partial in entryStates) add(ReadOnlyTimelineStateMarker.Partial)
            if (ReadOnlyTimelineEntryState.Stale in entryStates) add(ReadOnlyTimelineStateMarker.Stale)
            if (ReadOnlyTimelineEntryState.NotDone in entryStates) add(ReadOnlyTimelineStateMarker.NotDone)
            if (ReadOnlyTimelineEntryState.Blocked in entryStates) add(ReadOnlyTimelineStateMarker.Blocked)
            if (ReadOnlyTimelineEntryState.Failed in entryStates) add(ReadOnlyTimelineStateMarker.Failed)
            if (ReadOnlyTimelineEntryState.Completed in entryStates) add(ReadOnlyTimelineStateMarker.Completed)
        }.toList()
    }
}

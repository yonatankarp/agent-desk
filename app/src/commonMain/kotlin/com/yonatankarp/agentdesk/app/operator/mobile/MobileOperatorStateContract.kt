package com.yonatankarp.agentdesk.app.operator.mobile

import com.yonatankarp.agentdesk.app.operator.OperatorState
import com.yonatankarp.agentdesk.app.operator.OperatorStatePresenter
import com.yonatankarp.agentdesk.app.operator.SampleOperatorState
import com.yonatankarp.agentdesk.app.operator.timeline.ReadOnlyTimelineEntryState
import com.yonatankarp.agentdesk.app.operator.timeline.ReadOnlyTimelineProjector
import com.yonatankarp.agentdesk.app.operator.timeline.ReadOnlyTimelineStateMarker
import com.yonatankarp.agentdesk.core.domain.entities.WorkItem
import com.yonatankarp.agentdesk.core.domain.events.WorkEvent
import com.yonatankarp.agentdesk.core.domain.projections.StaleWorkAttention
import com.yonatankarp.agentdesk.core.domain.projections.WorkEventProjector

object MobileOperatorStateContract {
    fun sample(): MobileOperatorState = fromState(SampleOperatorState.current())

    fun fromState(
        state: OperatorState,
        projectionWarnings: List<MobileProjectionWarning> = emptyList(),
    ): MobileOperatorState {
        val evidenceByWorkItem = state.latestEvidenceByWorkItem()
        val workItemsById = state.workItems.associateBy { it.id.toString() }
        val recentEvents = OperatorStatePresenter.eventLines(state).map { line ->
            MobileEventLine(
                occurredAt = line.occurredAt,
                type = line.type,
                workItemId = line.workItemId,
                detail = line.detail,
                source = line.source,
                evidenceReferences = line.evidenceReferences.map { evidence ->
                    MobileEvidenceReference(
                        kind = evidence.kind,
                        label = evidence.label,
                        target = evidence.target,
                    )
                },
            )
        }
        val timelineProjection = ReadOnlyTimelineProjector.project(state)
        val timeline = timelineProjection.entries.map { entry ->
            MobileTimelineEntry(
                eventId = entry.eventId,
                occurredAt = entry.occurredAt,
                timeWindow = entry.timeWindow,
                source = entry.source,
                workItemId = entry.workItemId,
                type = entry.type,
                statusLabel = entry.status,
                stateLabel = entry.state.toMobileLabel(),
                summary = entry.summary,
                completionSummary = entry.completionSummary,
                evidenceReferences = entry.evidenceReferences.map { evidence ->
                    MobileEvidenceReference(
                        kind = evidence.kind,
                        label = evidence.label,
                        target = evidence.target,
                    )
                },
            )
        }

        return MobileOperatorState(
            currentWork = state.workItems
                .filter { !it.status.isTerminal }
                .map { item -> item.toMobileWorkItem(evidenceByWorkItem[item.id.toString()].orEmpty()) },
            attentionQueue = state.attentionItems(evidenceByWorkItem) +
                state.staleAttentionItems(workItemsById, evidenceByWorkItem),
            recentEvents = recentEvents,
            projectionWarnings = projectionWarnings,
            timeline = timeline,
            timelineStatusMarkers = timelineProjection.stateMarkers.map { it.toMobileLabel() },
            evidenceDetails = timeline.map { entry -> entry.toEvidenceDetail(recentEvents) },
        )
    }

    private fun MobileTimelineEntry.toEvidenceDetail(
        recentEvents: List<MobileEventLine>,
    ): MobileEvidenceDetail = MobileEvidenceDetail(
        eventId = eventId,
        source = source,
        timestamp = occurredAt,
        summary = summary,
        provenance = "replay event $eventId",
        evidenceReferences = evidenceReferences,
        relatedEvents = recentEvents.filter { line ->
            line.workItemId == workItemId && !(line.occurredAt == occurredAt && line.type == type)
        },
    )

    private fun ReadOnlyTimelineEntryState.toMobileLabel(): String = when (this) {
        ReadOnlyTimelineEntryState.ReadOnly -> "Read-only"
        ReadOnlyTimelineEntryState.NotDone -> "Not done"
        ReadOnlyTimelineEntryState.Blocked -> "Blocked"
        ReadOnlyTimelineEntryState.Failed -> "Failed"
        ReadOnlyTimelineEntryState.Completed -> "Completed"
        ReadOnlyTimelineEntryState.Partial -> "Partial"
        ReadOnlyTimelineEntryState.Stale -> "Stale"
    }

    private fun ReadOnlyTimelineStateMarker.toMobileLabel(): String = when (this) {
        ReadOnlyTimelineStateMarker.Empty -> "Empty"
        ReadOnlyTimelineStateMarker.ReadOnly -> "Read-only"
        ReadOnlyTimelineStateMarker.NotDone -> "Not done"
        ReadOnlyTimelineStateMarker.Blocked -> "Blocked"
        ReadOnlyTimelineStateMarker.Failed -> "Failed"
        ReadOnlyTimelineStateMarker.Completed -> "Completed"
        ReadOnlyTimelineStateMarker.Partial -> "Partial"
        ReadOnlyTimelineStateMarker.Stale -> "Stale"
    }

    fun fromEvents(events: List<WorkEvent>): MobileOperatorState {
        val projection = WorkEventProjector.project(events)
        val state = OperatorState.from(projection)

        return fromState(
            state = state,
            projectionWarnings = projection.ignoredEvents.map { issue ->
                MobileProjectionWarning(
                    eventId = issue.eventId.toString(),
                    reason = issue.reason,
                )
            },
        )
    }

    private fun OperatorState.latestEvidenceByWorkItem(): Map<String, List<MobileEvidenceReference>> = events
        .groupBy { it.workItemId.toString() }
        .mapValues { (_, events) ->
            events
                .lastOrNull { it.evidenceReferences.isNotEmpty() }
                ?.evidenceReferences
                ?.map { evidence ->
                    MobileEvidenceReference(
                        kind = evidence.kind.wireName,
                        label = evidence.label.toString(),
                        target = evidence.target.toString(),
                    )
                }
                .orEmpty()
        }

    private fun OperatorState.attentionItems(
        evidenceByWorkItem: Map<String, List<MobileEvidenceReference>>,
    ): List<MobileAttentionItem> = OperatorStatePresenter.attentionItems(this).map { item ->
        MobileAttentionItem(
            workItem = item.toMobileWorkItem(evidenceByWorkItem[item.id.toString()].orEmpty()),
            reason = item.summary?.toString(),
        )
    }

    private fun OperatorState.staleAttentionItems(
        workItemsById: Map<String, WorkItem>,
        evidenceByWorkItem: Map<String, List<MobileEvidenceReference>>,
    ): List<MobileAttentionItem> = OperatorStatePresenter.staleAttentionItems(this).mapNotNull { stale ->
        val item = workItemsById[stale.workItemId.toString()] ?: return@mapNotNull null
        MobileAttentionItem(
            workItem = item.toMobileWorkItem(evidenceByWorkItem[item.id.toString()].orEmpty()),
            reason = item.summary?.toString(),
            stale = stale.toMobileStaleAttention(),
        )
    }

    private fun WorkItem.toMobileWorkItem(
        evidenceReferences: List<MobileEvidenceReference>,
    ): MobileWorkItem {
        val presentation = OperatorStatePresenter.presentationFor(status)
        return MobileWorkItem(
            id = id.toString(),
            title = title.toString(),
            summary = summary?.toString(),
            status = MobileStatusPresentation(
                label = presentation.label,
                tone = presentation.tone,
            ),
            evidenceReferences = evidenceReferences,
        )
    }

    private fun StaleWorkAttention.toMobileStaleAttention(): MobileStaleAttention = MobileStaleAttention(
        lastEventAt = lastEventAt.toString(),
        staleForMinutes = staleForMinutes,
    )
}

package com.yonatankarp.agentdesk.app.operator.mobile

import com.yonatankarp.agentdesk.app.operator.OperatorState
import com.yonatankarp.agentdesk.app.operator.OperatorStatePresenter
import com.yonatankarp.agentdesk.app.operator.SampleOperatorState
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

        return MobileOperatorState(
            currentWork = state.workItems
                .filter { !it.status.isTerminal }
                .map { item -> item.toMobileWorkItem(evidenceByWorkItem[item.id.toString()].orEmpty()) },
            attentionQueue = state.attentionItems(evidenceByWorkItem) +
                state.staleAttentionItems(workItemsById, evidenceByWorkItem),
            recentEvents = OperatorStatePresenter.eventLines(state).map { line ->
                MobileEventLine(
                    occurredAt = line.occurredAt,
                    type = line.type,
                    workItemId = line.workItemId,
                    detail = line.detail,
                    evidenceReferences = line.evidenceReferences.map { evidence ->
                        MobileEvidenceReference(
                            kind = evidence.kind,
                            label = evidence.label,
                            target = evidence.target,
                        )
                    },
                )
            },
            projectionWarnings = projectionWarnings,
        )
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

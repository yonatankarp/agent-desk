package com.yonatankarp.agentdesk.app.operator

import com.yonatankarp.agentdesk.core.domain.entities.WorkItem
import com.yonatankarp.agentdesk.core.domain.events.WorkBlockedPayload
import com.yonatankarp.agentdesk.core.domain.events.WorkCanceledPayload
import com.yonatankarp.agentdesk.core.domain.events.WorkEventPayload
import com.yonatankarp.agentdesk.core.domain.events.WorkFailedPayload
import com.yonatankarp.agentdesk.core.domain.events.WorkNeedsDecisionPayload
import com.yonatankarp.agentdesk.core.domain.events.WorkStartedPayload
import com.yonatankarp.agentdesk.core.domain.events.WorkSucceededPayload
import com.yonatankarp.agentdesk.core.domain.projections.StaleWorkAttention
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkStatus

object OperatorStatePresenter {
    fun activeCount(state: OperatorState): Int = state.workItems.count { !it.status.isTerminal }

    fun attentionItems(state: OperatorState): List<WorkItem> = state.workItems.filter { it.status.requiresHumanAttention }

    fun staleAttentionItems(state: OperatorState): List<StaleWorkAttention> = state.staleAttention

    fun eventLines(state: OperatorState): List<EventLine> = state.events.map { event ->
        EventLine(
            occurredAt = event.occurredAt.toString(),
            type = event.type.wireName,
            workItemId = event.workItemId.toString(),
            source = event.source.toString(),
            detail = event.payload.describe(),
            evidenceReferences = event.evidenceReferences.map { evidence ->
                EvidenceLine(
                    kind = evidence.kind.wireName,
                    label = evidence.label.toString(),
                    target = evidence.target.toString(),
                )
            },
        )
    }

    fun presentationFor(status: WorkStatus): StatusPresentation = when (status) {
        WorkStatus.Queued -> StatusPresentation("Queued", StatusTone.Neutral)
        WorkStatus.Running -> StatusPresentation("Running", StatusTone.Active)
        WorkStatus.Waiting -> StatusPresentation("Waiting", StatusTone.Neutral)
        WorkStatus.NeedsDecision -> StatusPresentation("Needs decision", StatusTone.Attention)
        WorkStatus.Blocked -> StatusPresentation("Blocked", StatusTone.Blocked)
        WorkStatus.Succeeded -> StatusPresentation("Succeeded", StatusTone.Success)
        WorkStatus.Failed -> StatusPresentation("Failed", StatusTone.Failure)
        WorkStatus.Canceled -> StatusPresentation("Canceled", StatusTone.Neutral)
    }

    private fun WorkEventPayload.describe(): String = when (this) {
        is WorkStartedPayload -> summary?.toString() ?: title.toString()
        is WorkNeedsDecisionPayload -> reason.toString()
        is WorkBlockedPayload -> reason.toString()
        WorkSucceededPayload -> "Succeeded"
        is WorkFailedPayload -> reason.toString()
        is WorkCanceledPayload -> reason?.toString() ?: "Canceled"
    }
}

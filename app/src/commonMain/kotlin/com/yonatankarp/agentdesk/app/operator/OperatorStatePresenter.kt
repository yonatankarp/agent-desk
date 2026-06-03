package com.yonatankarp.agentdesk.app.operator

import com.yonatankarp.agentdesk.core.domain.entities.WorkItem
import com.yonatankarp.agentdesk.core.domain.events.WorkBlockedPayload
import com.yonatankarp.agentdesk.core.domain.events.WorkEventPayload
import com.yonatankarp.agentdesk.core.domain.events.WorkStartedPayload
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkStatus

object OperatorStatePresenter {
    fun activeCount(state: OperatorState): Int = state.workItems.count { !it.status.isTerminal }

    fun attentionItems(state: OperatorState): List<WorkItem> = state.workItems.filter { it.status.requiresHumanAttention }

    fun eventLines(state: OperatorState): List<EventLine> = state.events.map { event ->
        EventLine(
            occurredAt = event.occurredAt.toString(),
            type = event.type.wireName,
            workItemId = event.workItemId.toString(),
            source = event.source.toString(),
            detail = event.payload.describe(),
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
        is WorkBlockedPayload -> reason.toString()
    }
}

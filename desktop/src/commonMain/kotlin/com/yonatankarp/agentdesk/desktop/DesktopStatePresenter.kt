package com.yonatankarp.agentdesk.desktop

import com.yonatankarp.agentdesk.core.WorkBlockedPayload
import com.yonatankarp.agentdesk.core.WorkEvent
import com.yonatankarp.agentdesk.core.WorkEventPayload
import com.yonatankarp.agentdesk.core.WorkItem
import com.yonatankarp.agentdesk.core.WorkStartedPayload
import com.yonatankarp.agentdesk.core.WorkStatus

object DesktopStatePresenter {
    fun activeCount(state: DesktopOperatorState): Int = state.workItems.count { !it.status.isTerminal }

    fun attentionItems(state: DesktopOperatorState): List<WorkItem> = state.workItems.filter { it.status.requiresHumanAttention }

    fun eventLines(state: DesktopOperatorState): List<EventLine> = state.events.map { event ->
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

data class EventLine(
    val occurredAt: String,
    val type: String,
    val workItemId: String,
    val source: String,
    val detail: String,
)

data class StatusPresentation(
    val label: String,
    val tone: StatusTone,
)

enum class StatusTone {
    Neutral,
    Active,
    Attention,
    Blocked,
    Success,
    Failure,
}

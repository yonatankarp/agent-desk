package com.yonatankarp.agentdesk.mobile

import com.yonatankarp.agentdesk.app.operator.EvidenceDisplayFormatter
import com.yonatankarp.agentdesk.app.operator.mobile.MobileEventLine
import com.yonatankarp.agentdesk.app.operator.mobile.MobileEvidenceReference
import com.yonatankarp.agentdesk.app.operator.mobile.MobileStaleAttention
import com.yonatankarp.agentdesk.app.operator.mobile.MobileWorkItem

internal object MobileDisplayText {
    const val APP_TITLE = "Agent Desk"
    const val CURRENT_WORK_TITLE = "Current work"
    const val ATTENTION_QUEUE_TITLE = "Attention queue"
    const val RECENT_EVENTS_TITLE = "Recent events"
    const val PROJECTION_WARNINGS_TITLE = "Projection warnings"

    const val NO_CURRENT_WORK = "No current work"
    const val NO_ITEMS_NEED_ATTENTION = "No items need attention"
    const val NO_RECENT_ACCEPTED_EVENTS = "No recent accepted events"

    fun summary(
        currentWorkCount: Int,
        attentionCount: Int,
    ): String = "$currentWorkCount current / $attentionCount attention"

    fun evidenceReferences(references: List<MobileEvidenceReference>): String = references.joinToString { reference ->
        EvidenceDisplayFormatter.format(
            kind = reference.kind,
            label = reference.label,
            target = reference.target,
        )
    }

    fun staleAttention(stale: MobileStaleAttention): String = "Stale ${stale.staleForMinutes}m since ${stale.lastEventAt}"

    fun workRow(item: MobileWorkItem): String = buildString {
        append("[${item.status.label}] ${item.id} ${item.title}")
        item.summary?.let { summary -> append(" - $summary") }
        if (item.evidenceReferences.isNotEmpty()) {
            append(" | Evidence: ${evidenceReferences(item.evidenceReferences)}")
        }
    }

    fun eventRow(event: MobileEventLine): String = buildString {
        append("${event.occurredAt} [${event.type}] ${event.workItemId} - ${event.detail}")
        if (event.evidenceReferences.isNotEmpty()) {
            append(" | Evidence: ${evidenceReferences(event.evidenceReferences)}")
        }
    }
}

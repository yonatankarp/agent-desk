package com.yonatankarp.agentdesk.mobile

import com.yonatankarp.agentdesk.app.operator.EvidenceDisplayFormatter
import com.yonatankarp.agentdesk.app.operator.OperatorDisplaySection
import com.yonatankarp.agentdesk.app.operator.StaleDisplayFormatter
import com.yonatankarp.agentdesk.app.operator.mobile.MobileEventLine
import com.yonatankarp.agentdesk.app.operator.mobile.MobileEvidenceDetail
import com.yonatankarp.agentdesk.app.operator.mobile.MobileEvidenceReference
import com.yonatankarp.agentdesk.app.operator.mobile.MobileStaleAttention
import com.yonatankarp.agentdesk.app.operator.mobile.MobileTimelineEntry
import com.yonatankarp.agentdesk.app.operator.mobile.MobileWorkItem

internal object MobileDisplayText {
    const val APP_TITLE = "Agent Desk"
    val CURRENT_WORK_TITLE = OperatorDisplaySection.WorkState.mobileLabel
    val ATTENTION_QUEUE_TITLE = OperatorDisplaySection.DecisionQueue.mobileLabel
    const val RECENT_EVENTS_TITLE = "Recent events"
    val TIMELINE_TITLE = OperatorDisplaySection.Timeline.mobileLabel
    val PROJECTION_WARNINGS_TITLE = OperatorDisplaySection.ReplayStatus.mobileLabel
    val EVIDENCE_DETAIL_TITLE = OperatorDisplaySection.EvidenceDetail.mobileLabel

    const val NO_CURRENT_WORK = "No current work"
    const val NO_ITEMS_NEED_ATTENTION = "No items need attention"
    const val NO_RECENT_ACCEPTED_EVENTS = "No recent accepted events"
    const val NO_TIMELINE_ENTRIES = "No timeline entries"
    const val NO_PROJECTION_WARNINGS = "No projection warnings"

    const val DETAILS_DISCLOSURE_COLLAPSED = "Details ▸"
    const val DETAILS_DISCLOSURE_EXPANDED = "Details ▾"
    const val EVIDENCE_MISSING = "Evidence missing: no public-safe evidence reference was attached."
    const val EVIDENCE_DETAIL_MISSING = "Evidence missing: no timeline evidence detail is available."
    const val RELATED_EVENTS_NONE = "Related events: none"
    const val REDACTED_EVIDENCE = "Redacted evidence: raw provider payloads are not rendered."

    fun timelineStatus(markers: List<String>): String = "Status: ${markers.joinToString()}"

    fun timelineRow(entry: MobileTimelineEntry): String = buildString {
        append("${entry.occurredAt} [${entry.type}] ${entry.workItemId} from ${entry.source} [${entry.stateLabel}] - ${entry.summary}")
        entry.completionSummary?.let { completion -> append(" ($completion)") }
        if (entry.evidenceReferences.isNotEmpty()) {
            append(" | Evidence: ${evidenceReferences(entry.evidenceReferences)}")
        }
    }

    fun evidenceDetailRows(detail: MobileEvidenceDetail): List<String> = buildList {
        add("Source: ${detail.source}")
        add("Timestamp: ${detail.timestamp}")
        add("Summary: ${detail.summary}")
        add("Provenance: ${detail.provenance}")
        if (detail.evidenceReferences.isEmpty()) {
            add(EVIDENCE_MISSING)
        } else {
            add("Evidence: ${evidenceReferences(detail.evidenceReferences)}")
        }
        if (detail.relatedEvents.isEmpty()) {
            add(RELATED_EVENTS_NONE)
        } else {
            add("Related events: ${detail.relatedEvents.joinToString { related -> related.type }}")
        }
        add(REDACTED_EVIDENCE)
    }

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

    fun staleAttention(stale: MobileStaleAttention): String = "Stale ${StaleDisplayFormatter.humanizeMinutes(stale.staleForMinutes)} " +
        "since ${StaleDisplayFormatter.humanizeTimestamp(stale.lastEventAt)}"

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

package com.yonatankarp.agentdesk.app.runtime

import com.yonatankarp.agentdesk.core.domain.events.EventSource
import com.yonatankarp.agentdesk.core.domain.events.EventTimestamp
import com.yonatankarp.agentdesk.core.domain.events.WorkBlockedPayload
import com.yonatankarp.agentdesk.core.domain.events.WorkEvent
import com.yonatankarp.agentdesk.core.domain.events.WorkEventId
import com.yonatankarp.agentdesk.core.domain.events.WorkStartedPayload
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemId
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemTitle
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkSummary

class SanitizedRuntimeObservationMapper {
    fun toWorkEvent(observation: RuntimeWorkObservation): WorkEvent {
        observation.requirePublicSafe()

        return WorkEvent(
            id = WorkEventId.parse(observation.eventId),
            occurredAt = EventTimestamp.parse(observation.occurredAt),
            source = EventSource.parse(observation.source),
            workItemId = WorkItemId.parse(observation.workItemId),
            payload = observation.toPayload(),
        )
    }

    private fun RuntimeWorkObservation.toPayload() = when (kind) {
        RuntimeWorkObservationKind.Started ->
            WorkStartedPayload(
                title = WorkItemTitle.parse(requireNotNull(title) { "Started observations require a title" }),
                summary = summary?.let(WorkSummary::parse),
            )

        RuntimeWorkObservationKind.Blocked ->
            WorkBlockedPayload(
                reason = WorkSummary.parse(requireNotNull(reason) { "Blocked observations require a reason" }),
            )
    }

    private fun RuntimeWorkObservation.requirePublicSafe() {
        listOf(
            "eventId" to eventId,
            "occurredAt" to occurredAt,
            "source" to source,
            "workItemId" to workItemId,
            "title" to title,
            "summary" to summary,
            "reason" to reason,
        ).forEach { (label, value) ->
            value?.requirePublicSafeRuntimeField(label)
        }
    }

    private fun String.requirePublicSafeRuntimeField(label: String) {
        val normalized = lowercase()
        require(blockedFragments.none { fragment -> normalized.contains(fragment) }) {
            "Runtime observation $label must be public-safe before crossing the adapter boundary"
        }
    }

    companion object {
        private val blockedFragments =
            listOf(
                "/home/",
                "\\users\\",
                "op://",
                "token",
                "secret",
                "discord",
                "openclaw",
            )
    }
}

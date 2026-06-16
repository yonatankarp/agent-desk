package com.yonatankarp.agentdesk.core.domain.events

import com.yonatankarp.agentdesk.core.domain.valueobjects.IdentifierGrammar
import com.yonatankarp.agentdesk.core.domain.valueobjects.PublicSafeTextPolicy
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemId
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkItemTitle
import com.yonatankarp.agentdesk.core.domain.valueobjects.WorkSummary

data class WorkEvent(
    val id: WorkEventId,
    val occurredAt: EventTimestamp,
    val source: EventSource,
    val workItemId: WorkItemId,
    val payload: WorkEventPayload,
    val evidenceReferences: List<EvidenceReference> = emptyList(),
    val provenance: WorkProvenance? = null,
) {
    val type: WorkEventType
        get() = payload.type
}

@JvmInline
value class WorkEventId private constructor(val value: String) {
    companion object {
        private val validPattern = "[a-z0-9][a-z0-9._:-]{0,95}".toRegex()

        fun parse(raw: String): WorkEventId {
            val normalized = raw.trim().lowercase()
            require(validPattern.matches(normalized)) {
                "Work event id must be 1-96 chars using lowercase letters, numbers, '.', '_', ':', or '-'"
            }
            PublicSafeTextPolicy.requirePublicSafe(normalized, fieldName = "Work event id")
            return WorkEventId(normalized)
        }
    }

    override fun toString(): String = value
}

@JvmInline
value class EventTimestamp private constructor(val value: String) : Comparable<EventTimestamp> {
    companion object {
        private const val SECONDS_LENGTH = 19
        private const val FRACTION_DIGITS = 9
        private val rfc3339UtcPattern =
            """\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d{1,9})?Z""".toRegex()

        /**
         * Parses an RFC 3339 UTC instant and canonicalizes fractional seconds by trimming
         * trailing zeros (`.500Z` -> `.5Z`, `.000Z` -> no fraction), so equal instants
         * written at different precision compare equal.
         */
        fun parse(raw: String): EventTimestamp {
            val normalized = raw.trim()
            require(rfc3339UtcPattern.matches(normalized)) {
                "Event timestamp must be an RFC 3339 UTC instant, for example 2026-06-02T21:00:00Z"
            }
            return EventTimestamp(canonical(normalized))
        }

        private fun canonical(value: String): String {
            val fraction = value.substring(SECONDS_LENGTH).removeSuffix("Z").trimEnd('0').trimEnd('.')
            return value.take(SECONDS_LENGTH) + fraction + "Z"
        }
    }

    override fun compareTo(other: EventTimestamp): Int {
        val secondsOrder = value.take(SECONDS_LENGTH).compareTo(other.value.take(SECONDS_LENGTH))
        if (secondsOrder != 0) return secondsOrder
        return paddedFraction().compareTo(other.paddedFraction())
    }

    private fun paddedFraction(): String = value
        .substring(SECONDS_LENGTH)
        .removePrefix(".")
        .removeSuffix("Z")
        .padEnd(FRACTION_DIGITS, '0')

    override fun toString(): String = value
}

@JvmInline
value class EventSource private constructor(val value: String) {
    companion object {
        fun parse(raw: String): EventSource = EventSource(
            IdentifierGrammar.normalize(
                raw = raw,
                fieldName = "Event source",
                errorMessage = "Event source must be a lowercase adapter-neutral identifier",
            ),
        )
    }

    override fun toString(): String = value
}

enum class WorkEventType(val wireName: String) {
    WorkStarted("work.started"),
    WorkNeedsDecision("work.needs-decision"),
    WorkBlocked("work.blocked"),
    WorkSucceeded("work.succeeded"),
    WorkFailed("work.failed"),
    WorkCanceled("work.canceled"),
}

sealed interface WorkEventPayload {
    val type: WorkEventType
}

data class WorkStartedPayload(
    val title: WorkItemTitle,
    val summary: WorkSummary? = null,
) : WorkEventPayload {
    override val type: WorkEventType = WorkEventType.WorkStarted
}

data class WorkBlockedPayload(
    val reason: WorkSummary,
) : WorkEventPayload {
    override val type: WorkEventType = WorkEventType.WorkBlocked
}

data class WorkNeedsDecisionPayload(
    val reason: WorkSummary,
) : WorkEventPayload {
    override val type: WorkEventType = WorkEventType.WorkNeedsDecision
}

data object WorkSucceededPayload : WorkEventPayload {
    override val type: WorkEventType = WorkEventType.WorkSucceeded
}

data class WorkFailedPayload(
    val reason: WorkSummary,
) : WorkEventPayload {
    override val type: WorkEventType = WorkEventType.WorkFailed
}

data class WorkCanceledPayload(
    val reason: WorkSummary? = null,
) : WorkEventPayload {
    override val type: WorkEventType = WorkEventType.WorkCanceled
}
